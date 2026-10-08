package com.matchday.user.service;

import java.time.Duration;
import java.time.Instant;

import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import com.matchday.user.entity.Role;
import com.matchday.user.entity.User;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TokenService {
    private static final Duration ACCESS_TOKEN_TTL = Duration.ofMinutes(15);
    private static final String ISSUER = "matchday-user-service";

    private final JwtEncoder jwtEncoder;

    public long accessTokenTtlSeconds() {
        return ACCESS_TOKEN_TTL.getSeconds();
    }

    // Mint an access token for the given user
    public String mintAccessToken(User user) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(ISSUER)
                .issuedAt(now)
                .expiresAt(now.plus(ACCESS_TOKEN_TTL))
                .subject(user.getId().toString())
                .claim("roles", user.getRoles().stream().map(Role::name).toList())
                .build();

        return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }
}
