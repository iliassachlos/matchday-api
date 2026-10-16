package com.matchday.ticket.dto;

import java.time.Instant;
import java.util.UUID;

public record TicketDetails(
        UUID ticketId,
        String holderEmail,
        String matchName,
        String venueName,
        Instant startsAt,
        String section,
        String rowLabel,
        int seatNumber
    ) {}
