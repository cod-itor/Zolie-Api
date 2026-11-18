package com.example.zolieapi.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AdminUserResponse(
        UUID id,
        String username,
        String displayName,
        String email,
        String role,
        String status,
        String ageGroup,
        Instant suspendedUntil,
        Instant createdAt,
        List<ReportResponse> reportsAgainst,
        List<ModerationActionResponse> actions
) {
}
