package com.example.zolieapi.dto.response;

import java.time.Instant;
import java.util.UUID;

public record UploadUrlResponse(
        UUID mediaId,
        String uploadUrl,
        Instant expiresAt
) {
}
