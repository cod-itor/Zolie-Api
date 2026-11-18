package com.example.zolieapi.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record CreateStoryRequest(
        @NotNull UUID mediaId,
        @Schema(allowableValues = {"everyone", "close_friends"}) String audience,
        @Valid @Schema(description = "At most one sticker of each type.") List<StickerRequest> stickers
) {
}
