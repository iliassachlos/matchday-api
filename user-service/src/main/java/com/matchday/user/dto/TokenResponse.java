package com.matchday.user.dto;

public record TokenResponse(
    String accessToken,
    String tokenType,
    long expiresIn
) {}
