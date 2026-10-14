package com.matchday.ticket.dto;

import java.util.UUID;

public record EventAvailability(
    UUID eventId,
    long availableSeats
) {}
