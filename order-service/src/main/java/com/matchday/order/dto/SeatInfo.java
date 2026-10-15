package com.matchday.order.dto;

import java.util.UUID;

public record SeatInfo(
    UUID id, 
    long priceCents, 
    String status
) {}
