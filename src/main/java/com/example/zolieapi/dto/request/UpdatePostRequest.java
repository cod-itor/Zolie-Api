package com.example.zolieapi.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.UUID;

public record UpdatePostRequest(
        String caption,
        List<UUID> taggedUserIds,
        @Schema(allowableValues = {"public", "followers", "close_friends", "private"}) String visibility,
        UUID placeId,
        Boolean commentsEnabled,
        Boolean hideLikeCount,
        @Schema(description = "Pin to or unpin from my profile (up to 3).") Boolean pinned
) {
}
