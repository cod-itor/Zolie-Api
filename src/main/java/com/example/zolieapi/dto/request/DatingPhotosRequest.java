package com.example.zolieapi.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record DatingPhotosRequest(
        @NotNull @Schema(description = "Uploaded media IDs (purpose dating) in display order, up to 6.") List<UUID> mediaIds
) {
}
