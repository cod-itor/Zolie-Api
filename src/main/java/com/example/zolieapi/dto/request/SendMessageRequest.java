package com.example.zolieapi.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

public record SendMessageRequest(
        @Schema(description = "A UUID made by the app. A retry with the same value returns the first message.") UUID clientMessageId,
        @Schema(allowableValues = {"text", "image", "voice", "shared_post", "shared_reel"}, example = "text") String type,
        @Schema(example = "Hello!") String text,
        UUID mediaId,
        UUID sharedPostId,
        UUID sharedReelId
) {
}
