package com.capsule.capsule.dto;

import com.capsule.capsule.ItemType;
import jakarta.validation.constraints.NotNull;

public record AddItemRequest(@NotNull ItemType type, String content, String s3Key, int sortOrder) {}
