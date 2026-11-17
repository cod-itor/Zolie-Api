package com.example.zolieapi.dto.response;

import java.time.Instant;
import java.util.UUID;

public record MessageResponse(
        UUID id,
        UUID conversationId,
        UUID senderId,
        UUID clientMessageId,
        String type,
        String text,
        MediaItemResponse media,
        UUID storyId,
        UUID sharedPostId,
        UUID sharedReelId,
        Instant createdAt
) {
}
