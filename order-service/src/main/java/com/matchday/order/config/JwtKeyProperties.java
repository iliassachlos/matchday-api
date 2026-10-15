package com.matchday.order.config;

import java.security.interfaces.RSAPublicKey;

import org.springframework.boot.context.properties.ConfigurationProperties;

// Bind the properties from application.yml to this class
@ConfigurationProperties(prefix = "jwt")
public record JwtKeyProperties(RSAPublicKey publicKey) {
}
