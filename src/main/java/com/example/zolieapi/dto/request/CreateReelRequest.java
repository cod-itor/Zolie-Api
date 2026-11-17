package com.example.zolieapi.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateReelRequest(
        @NotNull UUID videoMediaId,
        String caption,
        @Schema(allowableValues = {"public", "followers"}) String visibility
) {
}
