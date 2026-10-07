package com.matchday.user.dto;

import com.matchday.user.entity.Role;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record UserResponse(
    UUID id, String email, String displayName, Set<Role> roles, Instant createdAt) {}
