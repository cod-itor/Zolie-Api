package com.example.zolieapi.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record StoryResponse(
        UUID id,
        UserSummaryResponse author,
        MediaItemResponse media,
        String audience,
        boolean seen,
        boolean isMine,
        Integer viewCount,
        List<StickerResponse> stickers,
        Instant createdAt,
        Instant expiresAt
) {
}
