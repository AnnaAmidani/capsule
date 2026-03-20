package com.capsule.capsule;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.*;

@Entity
@Table(name = "capsules")
public class Capsule {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID ownerId;

    @Column(nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CapsuleState state = CapsuleState.draft;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CapsuleVisibility visibility = CapsuleVisibility.public_;

    @Column(nullable = false)
    private Instant openAt;

    private String encryptionKeyHint;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    @OneToMany(mappedBy = "capsule", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("sortOrder ASC")
    private List<CapsuleItem> items = new ArrayList<>();

    @PreUpdate void onUpdate() { this.updatedAt = Instant.now(); }

    public UUID getId() { return id; }
    public UUID getOwnerId() { return ownerId; }
    public void setOwnerId(UUID ownerId) { this.ownerId = ownerId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public CapsuleState getState() { return state; }
    public void setState(CapsuleState state) { this.state = state; }
    public CapsuleVisibility getVisibility() { return visibility; }
    public void setVisibility(CapsuleVisibility visibility) { this.visibility = visibility; }
    public Instant getOpenAt() { return openAt; }
    public void setOpenAt(Instant openAt) { this.openAt = openAt; }
    public String getEncryptionKeyHint() { return encryptionKeyHint; }
    public void setEncryptionKeyHint(String encryptionKeyHint) { this.encryptionKeyHint = encryptionKeyHint; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public List<CapsuleItem> getItems() { return items; }
}
