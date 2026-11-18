package com.example.zolieapi.dto.response;

import java.time.Instant;
import java.util.UUID;

public record CommentResponse(
        UUID id,
        UserSummaryResponse author,
        String text,
        UUID parentId,
        long replyCount,
        int likeCount,
        boolean likedByMe,
        boolean isMine,
        boolean isAuthor,
        Instant createdAt
) {
}
