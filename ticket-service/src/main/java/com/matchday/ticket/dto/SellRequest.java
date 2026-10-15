package com.matchday.ticket.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record SellRequest(
    @NotNull UUID orderId
) {}
