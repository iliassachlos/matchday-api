package com.matchday.order.dto;

import java.util.UUID;

public record UserInfo(
    UUID id,
    String email,
    String displayName
) {}
