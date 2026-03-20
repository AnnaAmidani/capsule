package com.capsule.delivery;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "recipients")
public class Recipient {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID capsuleId;

    @Column(nullable = false)
    private String email;

    private UUID userId;
    private String accessToken;
    private Instant tokenExpiresAt;
    private Instant notifiedAt;
    private String deliveryError;
    private Instant accessedAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public UUID getId() { return id; }
    public UUID getCapsuleId() { return capsuleId; }
    public void setCapsuleId(UUID capsuleId) { this.capsuleId = capsuleId; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
    public Instant getTokenExpiresAt() { return tokenExpiresAt; }
    public void setTokenExpiresAt(Instant tokenExpiresAt) { this.tokenExpiresAt = tokenExpiresAt; }
    public Instant getNotifiedAt() { return notifiedAt; }
    public void setNotifiedAt(Instant notifiedAt) { this.notifiedAt = notifiedAt; }
    public String getDeliveryError() { return deliveryError; }
    public void setDeliveryError(String deliveryError) { this.deliveryError = deliveryError; }
    public Instant getAccessedAt() { return accessedAt; }
    public void setAccessedAt(Instant accessedAt) { this.accessedAt = accessedAt; }
    public Instant getCreatedAt() { return createdAt; }
}
