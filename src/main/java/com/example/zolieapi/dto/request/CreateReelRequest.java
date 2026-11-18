package com.example.zolieapi.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record CreateReelRequest(
        @NotNull UUID videoMediaId,
        String caption,
        @Schema(allowableValues = {"public", "followers", "close_friends", "private"}) String visibility,
        UUID placeId,
        @Schema(description = "Leave empty to create an original sound.") UUID soundId,
        Boolean commentsEnabled,
        List<UUID> taggedUserIds
) {
}
