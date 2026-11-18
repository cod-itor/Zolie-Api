package com.example.zolieapi.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record DatingStatusRequest(
        @NotBlank @Schema(allowableValues = {"on", "paused", "off"}) String status
) {
}
