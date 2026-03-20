package com.capsule.delivery;

import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class TokenServiceTest {

    TokenService tokenService = new TokenService("test-secret-32-chars-minimum-here!!");

    @Test
    void generatedTokenIsVerifiable() {
        var capsuleId = UUID.randomUUID();
        var email = "test@example.com";
        var expires = Instant.now().plus(7, ChronoUnit.DAYS);
        var token = tokenService.generate(capsuleId, email, expires);
        assertThat(tokenService.verify(token, capsuleId, email, expires)).isTrue();
    }

    @Test
    void tamperedTokenFailsVerification() {
        var capsuleId = UUID.randomUUID();
        var email = "test@example.com";
        var expires = Instant.now().plus(7, ChronoUnit.DAYS);
        var token = tokenService.generate(capsuleId, email, expires);
        assertThat(tokenService.verify(token + "x", capsuleId, email, expires)).isFalse();
    }
}
