package com.matchday.ticket.dto;

import java.util.UUID;

import com.matchday.ticket.entity.SeatStatus;

public record SeatResponse(
        UUID id,
        String section,
        String rowLabel,
        Integer number,
        long priceCents,
        SeatStatus status) {

    public static SeatResponse from(com.matchday.ticket.entity.Seat seat) {
        return new SeatResponse(
                seat.getId(),
                seat.getSection(),
                seat.getRowLabel(),
                seat.getNumber(),
                seat.getPriceCents(),
                seat.getStatus());
    }
}
