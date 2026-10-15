package com.matchday.ticket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.matchday.ticket.entity.Event;
import com.matchday.ticket.entity.Seat;
import com.matchday.ticket.entity.SeatStatus;
import com.matchday.ticket.entity.Venue;
import com.matchday.ticket.repository.EventRepository;
import com.matchday.ticket.repository.SeatRepository;
import com.matchday.ticket.repository.VenueRepository;

class SeatSaleTest extends IntegrationTestBase {

    private static final UUID MARIA = UUID.randomUUID();
    private static final UUID GIORGOS = UUID.randomUUID();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private VenueRepository venueRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UUID seatId;

    @BeforeEach
    void setUp() {
        seatRepository.deleteAllInBatch();
        eventRepository.deleteAllInBatch();
        venueRepository.deleteAllInBatch();

        Venue venue = venueRepository.save(Venue.create("OAKA", "Athens"));

        Event event = eventRepository.save(
                Event.create(venue, "Greece vs Netherlands", Instant.parse("2026-11-12T19:45:00Z")));

        Seat seat = seatRepository.save(Seat.create(event, "14", "8", 23, 4500L));

        seatId = seat.getId();
    }

    private RequestPostProcessor as(UUID userId) {
        return jwt().jwt(builder -> builder.subject(userId.toString()));
    }

    private void reserveAs(UUID userId) throws Exception {
        mockMvc.perform(post("/api/v1/seats/{id}/reserve", seatId).with(as(userId)))
                .andExpect(status().isOk());
    }

    private ResultActions sellAs(UUID userId, UUID orderId) throws Exception {
        return mockMvc.perform(post("/api/v1/seats/{id}/sell", seatId)
                .with(as(userId))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"orderId\":\"" + orderId + "\"}"));
    }

    @Test
    @DisplayName("Maria can buy the seat she is holding")
    void holderCanBuyTheirSeat() throws Exception {
        UUID orderId = UUID.randomUUID();
        reserveAs(MARIA);

        sellAs(MARIA, orderId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SOLD"))
                .andExpect(jsonPath("$.priceCents").value(4500));

        Seat sold = seatRepository.findById(seatId).orElseThrow();
        assertThat(sold.getStatus()).isEqualTo(SeatStatus.SOLD);
        assertThat(sold.getOrderId()).isEqualTo(orderId);
        assertThat(sold.getReservedUntil()).isNull();
    }

    @Test
    @DisplayName("retrying the same order is harmless: same answer, still one sale")
    void retryingTheSameOrderIsIdempotent() throws Exception {
        UUID orderId = UUID.randomUUID();
        reserveAs(MARIA);

        sellAs(MARIA, orderId).andExpect(status().isOk());
        sellAs(MARIA, orderId).andExpect(status().isOk());

        assertThat(seatRepository.findById(seatId).orElseThrow().getOrderId()).isEqualTo(orderId);
    }

    @Test
    @DisplayName("a sold seat cannot be sold again under a different order")
    void seatCannotBeSoldTwice() throws Exception {
        UUID firstOrder = UUID.randomUUID();
        reserveAs(MARIA);

        sellAs(MARIA, firstOrder).andExpect(status().isOk());
        sellAs(MARIA, UUID.randomUUID()).andExpect(status().isConflict());

        assertThat(seatRepository.findById(seatId).orElseThrow().getOrderId()).isEqualTo(firstOrder);
    }

    @Test
    @DisplayName("Giorgos cannot buy the seat Maria is holding")
    void someoneElsesHoldCannotBeBought() throws Exception {
        reserveAs(MARIA);

        sellAs(GIORGOS, UUID.randomUUID()).andExpect(status().isConflict());

        Seat seat = seatRepository.findById(seatId).orElseThrow();
        assertThat(seat.getStatus()).isEqualTo(SeatStatus.RESERVED);
        assertThat(seat.getReservedBy()).isEqualTo(MARIA);
    }

    @Test
    @DisplayName("an expired hold cannot be bought, even before the release job has run")
    void expiredHoldCannotBeBought() throws Exception {
        reserveAs(MARIA);

        // Simulate the clock passing the expiry; the row is still RESERVED, as it would
        // be between job runs
        jdbcTemplate.update("UPDATE seats SET reserved_until = now() - interval '1 second' WHERE id = ?", seatId);

        sellAs(MARIA, UUID.randomUUID()).andExpect(status().isConflict());
    }

    @Test
    @DisplayName("a seat nobody is holding cannot be bought")
    void unheldSeatCannotBeBought() throws Exception {
        sellAs(MARIA, UUID.randomUUID()).andExpect(status().isConflict());
    }
}
