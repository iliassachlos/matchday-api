package com.matchday.ticket.dto;

import jakarta.validation.constraints.NotBlank;

public record ScanRequest(
    @NotBlank String code
) {}
