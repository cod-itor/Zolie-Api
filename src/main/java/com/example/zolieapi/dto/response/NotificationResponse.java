package com.example.zolieapi.dto.response;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        String type,
        UserSummaryResponse actor,
        String targetType,
        UUID targetId,
        boolean read,
        Instant createdAt
) {
}
