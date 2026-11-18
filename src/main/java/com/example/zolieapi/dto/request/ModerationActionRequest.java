package com.example.zolieapi.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record ModerationActionRequest(
        @NotBlank @Schema(allowableValues = {"remove", "warn", "suspend", "dismiss"}) String action,
        @Schema(description = "Only for suspend. Leave empty for a permanent suspension.") Instant suspendedUntil,
        @Size(max = 500) String note
) {
}
