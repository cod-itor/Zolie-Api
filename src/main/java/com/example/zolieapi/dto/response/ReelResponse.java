package com.example.zolieapi.dto.response;

import java.time.Instant;
import java.util.UUID;

public record ReelResponse(
        UUID id,
        String caption,
        String status,
        String visibility,
        UserSummaryResponse author,
        String videoUrl,
        String thumbnailUrl,
        int durationMs,
        int likeCount,
        int commentCount,
        int shareCount,
        long viewCount,
        boolean likedByMe,
        boolean savedByMe,
        boolean isMine,
        Instant createdAt
) {
}
