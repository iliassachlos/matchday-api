package com.matchday.order.dto;

import java.time.Instant;
import java.util.UUID;

import com.matchday.order.entity.Order;

public record OrderResponse(
        UUID id,
        UUID seatId,
        long amountCents,
        String status,
        Instant createdAt) {

    public static OrderResponse from(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getSeatId(),
                order.getAmountCents(),
                order.getStatus().name(),
                order.getCreatedAt());
    }
}
