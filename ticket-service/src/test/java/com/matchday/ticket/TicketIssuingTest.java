package com.matchday.ticket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;

import com.matchday.ticket.dto.OrderPaidEvent;
import com.matchday.ticket.entity.Event;
import com.matchday.ticket.entity.Seat;
import com.matchday.ticket.entity.Ticket;
import com.matchday.ticket.entity.TicketStatus;
import com.matchday.ticket.entity.Venue;
import com.matchday.ticket.repository.EventRepository;
import com.matchday.ticket.repository.SeatRepository;
import com.matchday.ticket.repository.TicketRepository;
import com.matchday.ticket.repository.VenueRepository;
import com.matchday.ticket.service.OrderPaidListenerService;
import com.matchday.ticket.service.TicketService;

import tools.jackson.databind.ObjectMapper;

class TicketIssuingTest extends IntegrationTestBase {

    private static final UUID MARIA = UUID.randomUUID();

    @Autowired
    private TicketService ticketService;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private VenueRepository venueRepository;

    private UUID seatId;
    private UUID orderId;

    @BeforeEach
    void setUp() {
        ticketRepository.deleteAllInBatch();
        seatRepository.deleteAllInBatch();
        eventRepository.deleteAllInBatch();
        venueRepository.deleteAllInBatch();

        Venue venue = venueRepository.save(Venue.create("OAKA", "Athens"));
        Event event = eventRepository.save(
                Event.create(venue, "Greece vs Netherlands", Instant.parse("2026-11-12T19:45:00Z")));

        // A seat Maria has already bought, as ticket-service would have it after /sell
        orderId = UUID.randomUUID();
        Seat seat = Seat.create(event, "14", "8", 23, 4500L);
        seat.reserveFor(MARIA, Instant.now().plusSeconds(600));
        seat.sellTo(orderId);
        seatId = seatRepository.save(seat).getId();
    }

    private OrderPaidEvent paid(UUID order) {
        return new OrderPaidEvent(order, MARIA, "maria@test.gr", seatId, 4500, Instant.now());
    }

    @Test
    @DisplayName("an 'order paid' announcement on Kafka becomes a valid ticket")
    void announcementBecomesTicket() {
        kafkaTemplate.send(OrderPaidListenerService.ORDER_PAID_TOPIC, orderId.toString(),
                objectMapper.writeValueAsString(paid(orderId)));

        // The listener works in the background, so wait for the ticket rather than sleeping
        await().atMost(Duration.ofSeconds(15))
                .untilAsserted(() -> assertThat(ticketRepository.findByOrderId(orderId)).isPresent());

        Ticket ticket = ticketRepository.findByOrderId(orderId).orElseThrow();
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.VALID);
        assertThat(ticket.getHolderEmail()).isEqualTo("maria@test.gr");
        assertThat(ticket.getSeatId()).isEqualTo(seatId);
    }

    @Test
    @DisplayName("the same announcement twice still gives exactly one ticket")
    void repeatedAnnouncementGivesOneTicket() {
        ticketService.issue(paid(orderId));
        ticketService.issue(paid(orderId));

        assertThat(ticketRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("no ticket for an order the seat was not sold to")
    void noTicketForTheWrongOrder() {
        UUID someOtherOrder = UUID.randomUUID();

        assertThatThrownBy(() -> ticketService.issue(paid(someOtherOrder)))
                .isInstanceOf(IllegalStateException.class);

        assertThat(ticketRepository.count()).isZero();
    }
}
