package com.example.zolieapi.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record RsvpRequest(
        @NotBlank @Schema(allowableValues = {"interested", "going"}) String status
) {
}
