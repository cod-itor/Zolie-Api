package com.example.zolieapi.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UploadUrlRequest(
        @NotBlank @Schema(allowableValues = {"image", "video", "audio"}) String kind,
        @NotBlank @Schema(example = "image/jpeg") String mimeType,
        @NotNull @Min(1) @Schema(example = "204800") Long sizeBytes,
        @NotBlank @Schema(allowableValues = {"post", "reel", "story", "avatar", "chat", "dating"}) String purpose,
        @Min(1) @Schema(description = "Required for videos and audio.") Integer durationMs,
        @Min(1) @Max(20000) Integer width,
        @Min(1) @Max(20000) Integer height
) {
}
