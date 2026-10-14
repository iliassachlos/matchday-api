package com.matchday.ticket;

import com.matchday.ticket.entity.Event;
import com.matchday.ticket.entity.Seat;
import com.matchday.ticket.entity.SeatStatus;
import com.matchday.ticket.entity.Venue;
import com.matchday.ticket.repository.EventRepository;
import com.matchday.ticket.repository.SeatRepository;
import com.matchday.ticket.repository.VenueRepository;
import com.matchday.ticket.service.SeatReservationService;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class SeatReservationConcurrencyTest extends IntegrationTestBase {

    private static final int CONTENDERS = 50;

    @Autowired
    private SeatReservationService reservationService;

    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private VenueRepository venueRepository;

    private UUID seatId;

    @BeforeEach
    void setUp() {
        seatRepository.deleteAll();
        eventRepository.deleteAll();
        venueRepository.deleteAll();

        Venue venue = venueRepository.save(Venue.create("OAKA", "Athens"));

        Event event = eventRepository.save(
                Event.create(venue, "Greece vs Netherlands", Instant.parse("2026-10-13T19:00:00Z")));

        Seat seat = seatRepository.save(Seat.create(event, "14", "8", 23, 4500L));

        seatId = seat.getId();
    }

    @Test
    void onlyOneUserCanReserveTheSameSeat() throws InterruptedException {
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch allDone = new CountDownLatch(CONTENDERS);

        AtomicInteger reserved = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();

        try (ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < CONTENDERS; i++) {
                pool.submit(() -> {
                    try {
                        startGate.await();
                        reservationService.reserve(seatId, UUID.randomUUID());
                        reserved.incrementAndGet();
                    } catch (Exception _) {
                        rejected.incrementAndGet();
                    } finally {
                        allDone.countDown();
                    }
                });
            }

            startGate.countDown();
            assertThat(allDone.await(60, TimeUnit.SECONDS)).isTrue();
        }

        System.out.printf("%n>>> reserved=%d  rejected=%d%n%n", reserved.get(), rejected.get());

        assertThat(reserved.get()).isEqualTo(1);
        assertThat(rejected.get()).isEqualTo(CONTENDERS - 1);
        assertThat(seatRepository.findById(seatId).orElseThrow().getStatus())
                .isEqualTo(SeatStatus.RESERVED);
    }
}
