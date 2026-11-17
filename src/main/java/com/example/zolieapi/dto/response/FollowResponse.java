package com.example.zolieapi.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record FollowResponse(
        @Schema(allowableValues = {"none", "requested", "following"}) String followStatus
) {
}
