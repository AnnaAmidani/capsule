package com.capsule.shared.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.flyway.enabled=false",
    "spring.quartz.job-store-type=memory",
    "spring.kafka.bootstrap-servers=localhost:9092",
    "spring.data.redis.host=localhost",
    "aws.s3.bucket=test-bucket",
    "aws.s3.endpoint=http://localhost:9000",
    "stripe.secret-key=sk_test_dummy",
    "stripe.webhook-secret=whsec_dummy",
    "jwt.secret=test-secret-must-be-at-least-32-chars!!",
    "jwt.access-token-expiry-minutes=15",
    "jwt.refresh-token-expiry-days=30"
})
class JwtServiceTest {

    @Autowired JwtService jwtService;

    @Test
    void generatedAccessTokenIsValidatable() {
        var userId = UUID.randomUUID();
        var token = jwtService.generateAccessToken(userId, "test@example.com", "seed", "test-corr-id");
        assertThat(jwtService.isValid(token)).isTrue();
    }

    @Test
    void extractsSubjectFromToken() {
        var userId = UUID.randomUUID();
        var token = jwtService.generateAccessToken(userId, "test@example.com", "seed", "test-corr-id");
        assertThat(jwtService.extractUserId(token)).isEqualTo(userId);
    }

    @Test
    void extractsTierFromToken() {
        var userId = UUID.randomUUID();
        var token = jwtService.generateAccessToken(userId, "test@example.com", "vessel", "test-corr-id");
        assertThat(jwtService.extractTier(token)).isEqualTo("vessel");
    }

    @Test
    void invalidTokenFailsValidation() {
        assertThat(jwtService.isValid("not.a.token")).isFalse();
    }

    @Test
    void extractsEmailFromToken() {
        var userId = UUID.randomUUID();
        var token = jwtService.generateAccessToken(userId, "user@example.com", "seed", "corr-123");
        assertThat(jwtService.extractEmail(token)).isEqualTo("user@example.com");
    }

    @Test
    void extractsCorrelationIdFromToken() {
        var userId = UUID.randomUUID();
        var token = jwtService.generateAccessToken(userId, "user@example.com", "seed", "corr-abc");
        assertThat(jwtService.extractCorrelationId(token)).isEqualTo("corr-abc");
    }

    @Test
    void expiredTokenFailsValidation() {
        // Create a JwtService with -1 minute expiry → token is already expired at generation time
        var expiredJwtService = new JwtService("test-secret-must-be-at-least-32-chars!!", -1L);
        var token = expiredJwtService.generateAccessToken(
                UUID.randomUUID(), "x@example.com", "seed", "corr-x");
        assertThat(jwtService.isValid(token)).isFalse();
    }
}
