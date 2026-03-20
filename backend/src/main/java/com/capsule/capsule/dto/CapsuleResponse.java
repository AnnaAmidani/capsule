package com.capsule.capsule.dto;

import com.capsule.capsule.*;
import java.time.Instant;
import java.util.*;

public record CapsuleResponse(
    UUID id, UUID ownerId, String title,
    CapsuleState state, CapsuleVisibility visibility,
    Instant openAt, Instant createdAt, List<ItemResponse> items
) {
    public record ItemResponse(UUID id, ItemType type, String content, String s3Key, int sortOrder) {}

    public static CapsuleResponse from(Capsule c) {
        var items = c.getItems().stream()
            .map(i -> new ItemResponse(i.getId(), i.getType(), i.getContent(), i.getS3Key(), i.getSortOrder()))
            .toList();
        return new CapsuleResponse(c.getId(), c.getOwnerId(), c.getTitle(),
            c.getState(), c.getVisibility(), c.getOpenAt(), c.getCreatedAt(), items);
    }
}
