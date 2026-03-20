package com.capsule.capsule.dto;

import com.capsule.capsule.CapsuleVisibility;
import jakarta.validation.constraints.*;
import java.time.Instant;

public record CreateCapsuleRequest(
    @NotBlank String title,
    @NotNull CapsuleVisibility visibility,
    @NotNull @Future Instant openAt
) {}
