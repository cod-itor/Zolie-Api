package com.example.zolieapi.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record EligibilityResponse(
        boolean eligible,
        @Schema(allowableValues = {"under_18", "no_profile", "paused", "off"}) String reason
) {
}
