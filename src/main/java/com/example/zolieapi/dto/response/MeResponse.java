package com.example.zolieapi.dto.response;

import java.time.Instant;
import java.util.UUID;

public record MeResponse(
        UUID id,
        String username,
        String displayName,
        String bio,
        String avatarUrl,
        String email,
        String accountType,
        String role,
        String status,
        String ageGroup,
        int followerCount,
        int followingCount,
        int postCount,
        SettingsResponse settings,
        DatingSummaryResponse dating,
        Instant createdAt
) {
}
