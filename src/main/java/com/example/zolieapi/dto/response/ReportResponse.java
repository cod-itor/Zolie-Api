package com.example.zolieapi.dto.response;

import java.time.Instant;
import java.util.UUID;

public record ReportResponse(
        UUID id,
        String targetType,
        UUID targetId,
        String reason,
        String details,
        String status,
        String resolutionNote,
        Instant createdAt,
        Instant reviewedAt
) {
}
