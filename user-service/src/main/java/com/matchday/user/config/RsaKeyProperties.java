package com.matchday.user.config;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

import org.springframework.boot.context.properties.ConfigurationProperties;

// Bind the properties from application.yml to this class
@ConfigurationProperties(prefix = "jwt")
public record RsaKeyProperties(
        RSAPublicKey publicKey,
        RSAPrivateKey privateKey) {
}