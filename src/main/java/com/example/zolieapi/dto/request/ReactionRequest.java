package com.example.zolieapi.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

public record ReactionRequest(
        @Schema(description = "One of the reaction emojis. Defaults to a heart.", example = "🙏") String reaction
) {
}
