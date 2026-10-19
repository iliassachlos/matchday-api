package com.matchday.ticket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.matchday.ticket.dto.OrderPaidEvent;
import com.matchday.ticket.entity.Event;
import com.matchday.ticket.entity.Seat;
import com.matchday.ticket.entity.TicketStatus;
import com.matchday.ticket.entity.Venue;
import com.matchday.ticket.exception.TicketAlreadyUsedException;
import com.matchday.ticket.exception.TicketNotFoundException;
import com.matchday.ticket.repository.EventRepository;
import com.matchday.ticket.repository.SeatRepository;
import com.matchday.ticket.repository.TicketRepository;
import com.matchday.ticket.repository.VenueRepository;
import com.matchday.ticket.service.TicketCodeService;
import com.matchday.ticket.service.TicketService;

class TicketScanTest extends IntegrationTestBase {

    private static final int GATES = 50;
    private static final UUID MARIA = UUID.randomUUID();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TicketService ticketService;

    @Autowired
    private TicketCodeService ticketCodeService;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private VenueRepository venueRepository;

    private UUID ticketId;
    private String code;

    @BeforeEach
    void setUp() {
        ticketRepository.deleteAllInBatch();
        seatRepository.deleteAllInBatch();
        eventRepository.deleteAllInBatch();
        venueRepository.deleteAllInBatch();

        Venue venue = venueRepository.save(Venue.create("OAKA", "Athens"));
        Event event = eventRepository.save(
                Event.create(venue, "Greece vs Netherlands", Instant.parse("2026-11-12T19:45:00Z")));

        // A seat Maria has bought, and the ticket issued for it
        UUID orderId = UUID.randomUUID();
        Seat seat = Seat.create(event, "14", "8", 23, 4500L);
        seat.reserveFor(MARIA, Instant.now().plusSeconds(600));
        seat.sellTo(orderId);
        UUID seatId = seatRepository.save(seat).getId();

        ticketId = ticketService.issue(
                new OrderPaidEvent(orderId, MARIA, "maria@test.gr", seatId, 4500, Instant.now())).getId();
        code = ticketCodeService.codeFor(ticketId);
    }

    @Test
    @DisplayName("the same QR scanned at many gates at once admits exactly one person")
    void onlyOneScanAdmits() throws InterruptedException {
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch allDone = new CountDownLatch(GATES);

        AtomicInteger admitted = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();

        try (ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < GATES; i++) {
                pool.submit(() -> {
                    try {
                        startGate.await();
                        ticketService.admit(code);
                        admitted.incrementAndGet();
                    } catch (TicketAlreadyUsedException _) {
                        rejected.incrementAndGet();
                    } catch (InterruptedException _) {
                        Thread.currentThread().interrupt();
                    } finally {
                        allDone.countDown();
                    }
                });
            }

            startGate.countDown();
            assertThat(allDone.await(60, TimeUnit.SECONDS)).isTrue();
        }

        System.out.printf("%n>>> admitted=%d  rejected=%d%n%n", admitted.get(), rejected.get());

        assertThat(admitted.get()).isEqualTo(1);
        assertThat(rejected.get()).isEqualTo(GATES - 1);
        assertThat(ticketRepository.findById(ticketId).orElseThrow().getStatus())
                .isEqualTo(TicketStatus.USED);
    }

    @Test
    @DisplayName("a second scan is rejected and says when the ticket was first used")
    void secondScanIsRejected() {
        ticketService.admit(code);

        assertThatThrownBy(() -> ticketService.admit(code))
                .isInstanceOf(TicketAlreadyUsedException.class)
                .extracting("usedAt")
                .isNotNull();
    }

    @Test
    @DisplayName("a forged code is rejected and leaves the ticket valid")
    void forgedCodeIsRejected() {
        String forged = ticketId + ".not-a-real-signature";

        assertThatThrownBy(() -> ticketService.admit(forged))
                .isInstanceOf(TicketNotFoundException.class);

        assertThat(ticketRepository.findById(ticketId).orElseThrow().getStatus())
                .isEqualTo(TicketStatus.VALID);
    }

    // ---- Over HTTP: status codes and who may scan ----

    private RequestPostProcessor as(String role) {
        return jwt().jwt(builder -> builder.subject(UUID.randomUUID().toString()))
                .authorities(new SimpleGrantedAuthority(role));
    }

    private MockHttpServletRequestBuilder scan(String scannedCode) {
        return post("/api/v1/tickets/scan")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\": \"" + scannedCode + "\"}");
    }

    @Test
    @DisplayName("staff scanning a valid ticket get 200 and where to send the holder")
    void staffScanAdmits() throws Exception {
        mockMvc.perform(scan(code).with(as("ROLE_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.section").value("14"))
                .andExpect(jsonPath("$.rowLabel").value("8"))
                .andExpect(jsonPath("$.seatNumber").value(23));
    }

    @Test
    @DisplayName("scanning a used ticket over HTTP gives 409 with the time of first use")
    void usedTicketGives409() throws Exception {
        mockMvc.perform(scan(code).with(as("ROLE_ADMIN"))).andExpect(status().isOk());

        mockMvc.perform(scan(code).with(as("ROLE_ADMIN")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Ticket already used"))
                .andExpect(jsonPath("$.usedAt").exists());
    }

    @Test
    @DisplayName("a forged code over HTTP gives 404")
    void forgedCodeGives404() throws Exception {
        mockMvc.perform(scan(ticketId + ".forged").with(as("ROLE_ADMIN")))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("a regular user cannot scan, and the ticket stays valid")
    void regularUserIsForbidden() throws Exception {
        mockMvc.perform(scan(code).with(as("ROLE_USER")))
                .andExpect(status().isForbidden());

        assertThat(ticketRepository.findById(ticketId).orElseThrow().getStatus())
                .isEqualTo(TicketStatus.VALID);
    }

    @Test
    @DisplayName("scanning without a token gives 401")
    void anonymousIsUnauthorized() throws Exception {
        mockMvc.perform(scan(code)).andExpect(status().isUnauthorized());
    }
}
