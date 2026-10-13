package com.matchday.ticket.dto;

import java.util.UUID;

public record ReservationResponse(
    UUID seatId,
    UUID userId,
    String status
) {}
