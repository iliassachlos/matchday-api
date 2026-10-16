package com.matchday.ticket.service;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class TicketCodeService {

    private static final String ALGORITHM = "HmacSHA256";

    private final SecretKeySpec key;

    public TicketCodeService(@Value("${matchday.tickets.signing-secret}") String secret) {
        this.key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGORITHM);
    }

    public String codeFor(UUID ticketId) {
        return ticketId + "." + sign(ticketId.toString());
    }

    /**
     * Check a scanned code.
     *
     * @param code - the text read from the QR code
     * @return the ticket id if the code is genuine, empty if it was forged or damaged
     */
    public Optional<UUID> verify(String code) {
        int dot = code.lastIndexOf('.');
        if (dot < 0) {
            return Optional.empty();
        }

        String id = code.substring(0, dot);
        String signature = code.substring(dot + 1);

        // Same-speed comparison, so timing cannot reveal how much of a forged signature was right
        boolean genuine = MessageDigest.isEqual(
                sign(id).getBytes(StandardCharsets.UTF_8),
                signature.getBytes(StandardCharsets.UTF_8));
        if (!genuine) {
            return Optional.empty();
        }

        try {
            return Optional.of(UUID.fromString(id));
        } catch (IllegalArgumentException _) {
            return Optional.empty();
        }
    }

    /**
     * Signs a value with the HMAC-SHA256 algorithm and the configured secret key.
     * 
     * @param value - the value to sign
     * @return the Base64-encoded signature
     */
    private String sign(String value) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(key);
            byte[] signature = mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(signature);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HMAC-SHA256 is not available", e);
        }
    }
}
