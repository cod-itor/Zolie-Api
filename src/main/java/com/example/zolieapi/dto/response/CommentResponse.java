package com.example.zolieapi.dto.response;

import java.time.Instant;
import java.util.UUID;

public record CommentResponse(
        UUID id,
        UserSummaryResponse author,
        String text,
        UUID parentId,
        long replyCount,
        boolean isMine,
        Instant createdAt
) {
}
