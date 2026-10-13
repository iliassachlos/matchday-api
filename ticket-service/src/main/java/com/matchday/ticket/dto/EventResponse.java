package com.matchday.ticket.dto;

import java.time.Instant;
import java.util.UUID;

import com.matchday.ticket.entity.Event;

public record EventResponse(
        UUID id,
        String name,
        Instant startsAt) {

    public static EventResponse from(Event event) {
        return new EventResponse(event.getId(), event.getName(), event.getStartsAt());
    }
}
