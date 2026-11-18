package com.example.zolieapi.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record MeResponse(
        UUID id,
        String username,
        String displayName,
        String khmerName,
        String bio,
        String websiteUrl,
        String city,
        String avatarUrl,
        String coverUrl,
        String email,
        String accountType,
        String role,
        String status,
        String ageGroup,
        int followerCount,
        int followingCount,
        int postCount,
        List<PlaceResponse> favoritePlaces,
        SettingsResponse settings,
        DatingSummaryResponse dating,
        Instant deleteAfter,
        Instant createdAt
) {
}
