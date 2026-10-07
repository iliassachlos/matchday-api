package com.matchday.user.dto;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import com.matchday.user.entity.Role;

public record UserResponse(
    UUID id,
    String email,
    String displayName,
    Set<Role> roles,
    Instant createdAt
) {}
