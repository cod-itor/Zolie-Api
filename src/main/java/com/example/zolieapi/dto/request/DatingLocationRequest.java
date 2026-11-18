package com.example.zolieapi.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record DatingLocationRequest(
        @NotNull @DecimalMin("-90") @DecimalMax("90") @Schema(example = "11.5564") Double lat,
        @NotNull @DecimalMin("-180") @DecimalMax("180") @Schema(example = "104.9282") Double lng
) {
}
