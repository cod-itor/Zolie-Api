package com.example.zolieapi.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ProfileResponse(
        UUID id,
        String username,
        String displayName,
        String khmerName,
        String bio,
        String websiteUrl,
        String city,
        String avatarUrl,
        String coverUrl,
        boolean isPrivate,
        boolean isMe,
        boolean canView,
        String followStatus,
        boolean followsMe,
        boolean isBlockedByMe,
        boolean isOnline,
        int followerCount,
        int followingCount,
        int postCount,
        List<PlaceResponse> favoritePlaces,
        List<UserSummaryResponse> mutualFollowers,
        long mutualFollowerCount,
        Instant joinedAt
) {
}
