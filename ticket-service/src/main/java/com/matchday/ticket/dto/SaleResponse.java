package com.matchday.ticket.dto;

import java.util.UUID;

import com.matchday.ticket.entity.Seat;

public record SaleResponse(
        UUID seatId,
        UUID orderId,
        long priceCents,
        String status) {

    public static SaleResponse from(Seat seat) {
        return new SaleResponse(seat.getId(), seat.getOrderId(), seat.getPriceCents(), seat.getStatus().name());

    }
}
