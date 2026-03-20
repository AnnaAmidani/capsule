package com.capsule.delivery.dto;

import java.time.Instant;
import java.util.UUID;

public record RecipientResponse(UUID id, String email, Instant notifiedAt, Instant accessedAt) {}
