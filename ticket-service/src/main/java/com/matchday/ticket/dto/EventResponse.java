package com.matchday.ticket.dto;

import java.time.Instant;
import java.util.UUID;

import com.matchday.ticket.entity.Event;

public record EventResponse(
        UUID id,
        String name,
        Instant startsAt,
        String venueName,
        String city,
        long availableSeats) {

    public static EventResponse from(Event event, long availableSeats) {
        return new EventResponse(
                event.getId(),
                event.getName(),
                event.getStartsAt(),
                event.getVenue().getName(),
                event.getVenue().getCity(),
                availableSeats);
    }
}
