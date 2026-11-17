package com.example.zolieapi.dto.response;

import java.util.UUID;

public record MediaItemResponse(
        UUID id,
        String kind,
        String url,
        Integer width,
        Integer height,
        Integer durationMs
) {
}
