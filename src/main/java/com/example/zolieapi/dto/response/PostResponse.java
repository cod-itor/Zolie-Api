package com.example.zolieapi.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PostResponse(
        UUID id,
        String type,
        String caption,
        String visibility,
        UserSummaryResponse author,
        List<MediaItemResponse> media,
        PollResponse poll,
        int likeCount,
        int commentCount,
        int shareCount,
        boolean likedByMe,
        boolean savedByMe,
        boolean isMine,
        Instant createdAt,
        Instant updatedAt
) {
}
