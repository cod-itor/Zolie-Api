package com.example.zolieapi.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record CreateStoryRequest(
        @Schema(description = "A photo or video. Leave empty for a text moment or a shared post.") UUID mediaId,
        @Size(max = 500) @Schema(description = "Text for a text moment.") String text,
        @Size(max = 20) String textBackground,
        @Schema(description = "Share one of my posts or a public post as a moment.") UUID sharedPostId,
        @Schema(allowableValues = {"everyone", "close_friends"}) String audience,
        @Valid @Schema(description = "At most one sticker of each type.") List<StickerRequest> stickers
) {
}
