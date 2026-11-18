package com.example.zolieapi.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CreateSoundRequest(
        @NotBlank @Size(max = 120) String title,
        @Size(max = 80) String artistName,
        @NotNull @Schema(description = "Uploaded audio (purpose sound).") UUID audioMediaId
) {
}
