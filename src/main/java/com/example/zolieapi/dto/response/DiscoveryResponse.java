package com.example.zolieapi.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record DiscoveryResponse(
        List<DatingCardResponse> items,
        @Schema(allowableValues = {"location_required", "no_matches_in_range"}) String reason
) {
}
