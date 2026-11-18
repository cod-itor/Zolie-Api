package com.example.zolieapi.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CreateReportRequest(
        @NotBlank @Schema(allowableValues = {"user", "post", "reel", "comment", "story", "message"}) String targetType,
        @NotNull UUID targetId,
        @NotBlank @Schema(allowableValues = {"spam", "harassment", "nudity", "violence", "hate", "underage", "scam", "fake_profile", "other"}) String reason,
        @Size(max = 1000) String details
) {
}
