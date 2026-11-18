package com.example.zolieapi.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

public record DeletionRequest(
        @Size(max = 200) @Schema(example = "I spend too much time here") String reason
) {
}
