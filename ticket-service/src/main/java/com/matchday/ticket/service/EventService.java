package com.matchday.ticket.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.matchday.ticket.dto.EventAvailability;
import com.matchday.ticket.dto.EventResponse;
import com.matchday.ticket.dto.SeatResponse;
import com.matchday.ticket.entity.SeatStatus;
import com.matchday.ticket.repository.EventRepository;
import com.matchday.ticket.repository.SeatRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;
    private final SeatRepository seatRepository;

    /**
     * List all events with the number of available seats for each event.
     * 
     * @return
     */
    @Transactional(readOnly = true)
    public List<EventResponse> listEvents() {
        Map<UUID, Long> availableByEvent = seatRepository.countByEventWithStatus(SeatStatus.AVAILABLE).stream()
                .collect(Collectors.toMap(EventAvailability::eventId, EventAvailability::availableSeats));

        return eventRepository.findAllWithVenue().stream()
                .map(event -> EventResponse.from(event, availableByEvent.getOrDefault(event.getId(), 0L)))
                .toList();
    }

    /**
     * List all seats for a given event.
     * 
     * @param eventId - Event ID
     * @return The list of seats for the given event
     */
    @Transactional(readOnly = true)
    public List<SeatResponse> listSeats(UUID eventId) {
        return seatRepository.findSeatMap(eventId).stream()
                .map(SeatResponse::from)
                .toList();
    }
}
