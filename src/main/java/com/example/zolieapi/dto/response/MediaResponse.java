package com.example.zolieapi.dto.response;

import java.util.UUID;

public record MediaResponse(
        UUID id,
        String kind,
        String purpose,
        String status,
        String url
) {
}
