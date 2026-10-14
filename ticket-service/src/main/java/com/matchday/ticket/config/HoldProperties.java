package com.matchday.ticket.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

// How long a reserved seat stays held before it is released back to sale
@ConfigurationProperties(prefix = "matchday.hold")
public record HoldProperties(Duration duration) {
}
