package com.example.zolieapi.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;

public record StickerRequest(
        @NotBlank @Schema(allowableValues = {"question", "emoji_slider", "countdown"}) String type,
        @Schema(description = "Question text or countdown title.") String prompt,
        @Schema(description = "Emoji for the slider.") String emoji,
        @Schema(description = "When the countdown ends.") Instant endsAt,
        @Schema(example = "0.5") Float x,
        @Schema(example = "0.5") Float y,
        Float scale,
        Float rotation
) {
}
