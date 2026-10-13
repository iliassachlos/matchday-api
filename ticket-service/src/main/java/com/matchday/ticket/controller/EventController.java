package com.matchday.ticket.controller;

import org.springframework.web.bind.annotation.RestController;

import com.matchday.ticket.dto.EventResponse;
import com.matchday.ticket.dto.SeatResponse;
import com.matchday.ticket.service.EventService;

import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@RestController
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    @GetMapping
    public List<EventResponse> listEvents() {
        return eventService.listEvents();
    }

    @GetMapping("/{eventId}/seats")
    public List<SeatResponse> listSeats(@PathVariable UUID eventId) {
        return eventService.listSeats(eventId);
    }

}
