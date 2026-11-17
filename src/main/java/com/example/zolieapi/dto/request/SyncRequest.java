package com.example.zolieapi.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record SyncRequest(
        @NotBlank @Schema(example = "dara.sok") String username,
        @NotBlank @Size(max = 50) @Schema(example = "Dara Sok") String displayName,
        @NotNull @Schema(example = "2000-05-17") LocalDate dateOfBirth,
        @Schema(example = "true") boolean termsAccepted
) {
}
