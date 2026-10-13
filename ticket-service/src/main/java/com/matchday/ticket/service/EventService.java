package com.matchday.ticket.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.matchday.ticket.dto.EventResponse;
import com.matchday.ticket.dto.SeatResponse;
import com.matchday.ticket.repository.EventRepository;
import com.matchday.ticket.repository.SeatRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;
    private final SeatRepository seatRepository;

    // List all events
    @Transactional(readOnly = true)
    public List<EventResponse> listEvents() {
        return eventRepository.findAll().stream().map(EventResponse::from).toList();
    }

    // List all seats for a specific event
    @Transactional(readOnly = true)
    public List<SeatResponse> listSeats(UUID eventId) {
        return seatRepository.findByEventIdOrderByNumberAsc(eventId).stream()
                .map(SeatResponse::from)
                .toList();
    }
}
