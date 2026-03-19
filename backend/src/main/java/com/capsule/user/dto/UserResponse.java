package com.capsule.user.dto;

import com.capsule.user.UserTier;
import java.util.UUID;

public record UserResponse(UUID id, String email, UserTier tier) {}
