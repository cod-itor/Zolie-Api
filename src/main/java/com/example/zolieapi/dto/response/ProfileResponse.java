package com.example.zolieapi.dto.response;

import java.util.UUID;

public record ProfileResponse(
        UUID id,
        String username,
        String displayName,
        String bio,
        String avatarUrl,
        boolean isPrivate,
        boolean isMe,
        boolean canView,
        String followStatus,
        boolean followsMe,
        boolean isBlockedByMe,
        int followerCount,
        int followingCount,
        int postCount
) {
}
