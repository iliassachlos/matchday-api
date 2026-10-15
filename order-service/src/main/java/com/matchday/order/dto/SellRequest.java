package com.matchday.order.dto;

import java.util.UUID;

public record SellRequest(
    UUID orderId
) {}
