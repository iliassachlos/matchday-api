package com.matchday.ticket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.matchday.ticket.entity.Event;
import com.matchday.ticket.entity.Seat;
import com.matchday.ticket.entity.SeatStatus;
import com.matchday.ticket.entity.Venue;
import com.matchday.ticket.repository.EventRepository;
import com.matchday.ticket.repository.SeatRepository;
import com.matchday.ticket.repository.TicketRepository;
import com.matchday.ticket.repository.VenueRepository;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

class SeatApiTest extends IntegrationTestBase {

        private static final UUID MARIA = UUID.randomUUID();
        private static final UUID GIORGOS = UUID.randomUUID();

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private EventRepository eventRepository;

        @Autowired
        private SeatRepository seatRepository;

        @Autowired
        private TicketRepository ticketRepository;

        @Autowired
        private VenueRepository venueRepository;

        private UUID eventId;
        private UUID seatId;

        @BeforeEach
        void setUp() {
                ticketRepository.deleteAllInBatch();
                seatRepository.deleteAllInBatch();
                eventRepository.deleteAllInBatch();
                venueRepository.deleteAllInBatch();

                Venue venue = venueRepository.save(Venue.create("OAKA", "Athens"));

                Event event = eventRepository.save(
                                Event.create(venue, "Greece vs Netherlands", Instant.parse("2026-10-13T19:00:00Z")));

                Seat seat = seatRepository.save(Seat.create(event, "14", "8", 23, 4500L));

                eventId = event.getId();
                seatId = seat.getId();
        }

        /**
         * Stands in for a validated RS256 token: ticket-service only ever verifies,
         * never mints.
         */
        private RequestPostProcessor as(UUID userId) {
                return jwt().jwt(builder -> builder.subject(userId.toString()));
        }

        @Test
        @DisplayName("the seat map lists the event's seats with their status")
        void seatMapIsListed() throws Exception {
                mockMvc.perform(get("/api/v1/events/{id}/seats", eventId).with(as(MARIA)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.length()").value(1))
                                .andExpect(jsonPath("$[0].section").value("14"))
                                .andExpect(jsonPath("$[0].number").value(23))
                                .andExpect(jsonPath("$[0].priceCents").value(4500))
                                .andExpect(jsonPath("$[0].status").value(SeatStatus.AVAILABLE.name()));
        }

        @Test
        @DisplayName("reserving a free seat succeeds and marks it RESERVED for the caller")
        void reserveSucceeds() throws Exception {
                mockMvc.perform(post("/api/v1/seats/{id}/reserve", seatId).with(as(MARIA)))
                                .andExpect(status().is2xxSuccessful());

                Seat stored = seatRepository.findById(seatId).orElseThrow();

                assertThat(stored.getStatus()).isEqualTo(SeatStatus.RESERVED);
                assertThat(stored.getReservedBy()).isEqualTo(MARIA);
        }

        @Test
        @DisplayName("reserving a taken seat is 409 and does not change the owner")
        void reserveTakenSeatIsConflict() throws Exception {
                mockMvc.perform(post("/api/v1/seats/{id}/reserve", seatId).with(as(MARIA)))
                                .andExpect(status().is2xxSuccessful());

                mockMvc.perform(post("/api/v1/seats/{id}/reserve", seatId).with(as(GIORGOS)))
                                .andExpect(status().isConflict());

                assertThat(seatRepository.findById(seatId).orElseThrow().getReservedBy()).isEqualTo(MARIA);
        }

        @Test
        @DisplayName("retrying a reservation you already hold returns the same hold, not 409 and not extended")
        void reserveRetryIsIdempotent() throws Exception {
                String first = mockMvc.perform(post("/api/v1/seats/{id}/reserve", seatId).with(as(MARIA)))
                                .andExpect(status().isOk())
                                .andReturn().getResponse().getContentAsString();

                String retry = mockMvc.perform(post("/api/v1/seats/{id}/reserve", seatId).with(as(MARIA)))
                                .andExpect(status().isOk())
                                .andReturn().getResponse().getContentAsString();

                assertThat(retry).isEqualTo(first);
        }

        @Test
        @DisplayName("reserving a seat that does not exist is 404, not 500")
        void reserveUnknownSeatIsNotFound() throws Exception {
                mockMvc.perform(post("/api/v1/seats/{id}/reserve", UUID.randomUUID()).with(as(MARIA)))
                                .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("reserving without a token is 401")
        void reserveRequiresAuthentication() throws Exception {
                mockMvc.perform(post("/api/v1/seats/{id}/reserve", seatId))
                                .andExpect(status().isUnauthorized());

                assertThat(seatRepository.findById(seatId).orElseThrow().getStatus())
                                .isEqualTo(SeatStatus.AVAILABLE);
        }

        @Test
        @DisplayName("Giorgos cannot see Maria's seats: /me is scoped to the token subject")
        void seatsAreScopedToTheCaller() throws Exception {
                mockMvc.perform(post("/api/v1/seats/{id}/reserve", seatId).with(as(MARIA)))
                                .andExpect(status().is2xxSuccessful());

                mockMvc.perform(get("/api/v1/seats/me").with(as(MARIA)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.length()").value(1))
                                .andExpect(jsonPath("$[0].number").value(23));

                mockMvc.perform(get("/api/v1/seats/me").with(as(GIORGOS)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.length()").value(0));
        }

        @Test
        @DisplayName("the event list shows the venue and drops to zero available seats when sold out")
        void eventListShowsVenueAndAvailability() throws Exception {
                mockMvc.perform(get("/api/v1/events").with(as(MARIA)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.length()").value(1))
                                .andExpect(jsonPath("$[0].venueName").value("OAKA"))
                                .andExpect(jsonPath("$[0].city").value("Athens"))
                                .andExpect(jsonPath("$[0].availableSeats").value(1));

                mockMvc.perform(post("/api/v1/seats/{id}/reserve", seatId).with(as(MARIA)))
                                .andExpect(status().is2xxSuccessful());

                mockMvc.perform(get("/api/v1/events").with(as(GIORGOS)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].availableSeats").value(0));
        }

}
