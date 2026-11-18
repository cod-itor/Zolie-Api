package com.example.zolieapi.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record DatingPreferencesRequest(
        @Schema(example = "[\"woman\", \"man\"]") List<String> genders,
        @Schema(example = "18") Integer minAge,
        @Schema(example = "35") Integer maxAge,
        @Schema(example = "25") Integer maxDistanceKm
) {
}
