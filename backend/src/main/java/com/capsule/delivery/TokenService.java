package com.capsule.delivery;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class TokenService {

    private final String secret;

    public TokenService(@Value("${jwt.secret}") String secret) {
        this.secret = secret;
    }

    public String generate(UUID capsuleId, String email, Instant expiresAt) {
        return hmac(payload(capsuleId, email, expiresAt));
    }

    public boolean verify(String token, UUID capsuleId, String email, Instant expiresAt) {
        var expected = generate(capsuleId, email, expiresAt);
        return expected.equals(token);
    }

    private String payload(UUID capsuleId, String email, Instant expiresAt) {
        return capsuleId + "|" + email + "|" + expiresAt.toEpochMilli();
    }

    private String hmac(String data) {
        try {
            var mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("HMAC error", e);
        }
    }
}
