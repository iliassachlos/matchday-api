package com.matchday.ticket.dto;

import java.time.Instant;
import java.util.UUID;

public record OrderPaidEvent(
        UUID orderId,
        UUID userId,
        String buyerEmail,
        UUID seatId,
        long amountCents,
        Instant paidAt
    ) {}
