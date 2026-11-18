package com.example.zolieapi.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

public record StickerAnswerRequest(
        @Schema(description = "Answer to a question sticker.") String text,
        @Schema(description = "Slider value from 0 to 100.") Integer value
) {
}
