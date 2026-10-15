package com.matchday.order.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

// No userId (it comes from the JWT) and no price (it comes from ticket-service)
public record PlaceOrderRequest(@NotNull UUID seatId, @NotBlank String paymentMethod) {
}
