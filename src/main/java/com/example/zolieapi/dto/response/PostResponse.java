package com.example.zolieapi.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
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
        PlaceResponse place,
        SoundResponse sound,
        String textBackground,
        String textStyle,
        int likeCount,
        int commentCount,
        int shareCount,
        @Schema(description = "Only for my own posts.") Long reachCount,
        boolean likedByMe,
        String myReaction,
        List<String> topReactions,
        boolean savedByMe,
        boolean isMine,
        boolean isPinned,
        boolean commentsEnabled,
        boolean hideLikeCount,
        Instant createdAt,
        Instant updatedAt
) {
}
