package com.matchday.ticket;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.matchday.ticket.dto.ReservationResponse;
import com.matchday.ticket.entity.Event;
import com.matchday.ticket.entity.Seat;
import com.matchday.ticket.entity.SeatStatus;
import com.matchday.ticket.entity.Venue;
import com.matchday.ticket.repository.EventRepository;
import com.matchday.ticket.repository.SeatRepository;
import com.matchday.ticket.repository.TicketRepository;
import com.matchday.ticket.repository.VenueRepository;
import com.matchday.ticket.service.SeatHoldReleaser;
import com.matchday.ticket.service.SeatReservationService;

class SeatHoldExpiryTest extends IntegrationTestBase {

    private static final UUID MARIA = UUID.randomUUID();
    private static final UUID GIORGOS = UUID.randomUUID();

    @Autowired
    private SeatReservationService reservationService;

    @Autowired
    private SeatHoldReleaser releaser;

    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private VenueRepository venueRepository;

    private UUID seatId;

    @BeforeEach
    void setUp() {
        ticketRepository.deleteAllInBatch();
        seatRepository.deleteAllInBatch();
        eventRepository.deleteAllInBatch();
        venueRepository.deleteAllInBatch();

        Venue venue = venueRepository.save(Venue.create("OAKA", "Athens"));

        Event event = eventRepository.save(
                Event.create(venue, "Greece vs Netherlands", Instant.parse("2026-11-12T19:45:00Z")));

        Seat seat = seatRepository.save(Seat.create(event, "14", "8", 23, 4500L));

        seatId = seat.getId();
    }

    @Test
    @DisplayName("a hold is kept until it expires, then the seat goes back on sale")
    void expiredHoldIsReleased() {
        ReservationResponse hold = reservationService.reserve(seatId, MARIA);

        assertThat(releaser.releaseExpiredHolds(hold.reservedUntil().minusSeconds(1))).isZero();
        assertThat(seatRepository.findById(seatId).orElseThrow().getStatus()).isEqualTo(SeatStatus.RESERVED);

        assertThat(releaser.releaseExpiredHolds(hold.reservedUntil().plusSeconds(1))).isEqualTo(1);

        Seat released = seatRepository.findById(seatId).orElseThrow();
        assertThat(released.getStatus()).isEqualTo(SeatStatus.AVAILABLE);
        assertThat(released.getReservedBy()).isNull();
        assertThat(released.getReservedUntil()).isNull();
    }

    @Test
    @DisplayName("after Maria's hold expires, Giorgos can take the seat")
    void releasedSeatCanBeReservedAgain() {
        ReservationResponse first = reservationService.reserve(seatId, MARIA);

        releaser.releaseExpiredHolds(first.reservedUntil().plusSeconds(1));

        ReservationResponse second = reservationService.reserve(seatId, GIORGOS);
        assertThat(second.userId()).isEqualTo(GIORGOS);
    }
}
