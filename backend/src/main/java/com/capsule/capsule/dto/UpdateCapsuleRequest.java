package com.capsule.capsule.dto;

import java.time.Instant;

public record UpdateCapsuleRequest(String title, Instant openAt) {}
