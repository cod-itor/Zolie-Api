package com.example.zolieapi.dto.response;

import java.time.Instant;
import java.util.UUID;

public record ModerationActionResponse(
        UUID id,
        String action,
        String targetType,
        UUID targetId,
        String note,
        UUID adminId,
        Instant createdAt
) {
}
