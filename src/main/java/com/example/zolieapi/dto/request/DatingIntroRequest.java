package com.example.zolieapi.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record DatingIntroRequest(
        @NotNull UUID toUserId,
        @NotBlank @Size(max = 500) @Schema(example = "Hi! I love Kampot too.") String message,
        @Schema(description = "The prompt answer being sparked, if any.") UUID promptAnswerId
) {
}
