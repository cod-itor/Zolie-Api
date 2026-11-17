package com.example.zolieapi.dto.response;

import java.util.UUID;

public record UserSummaryResponse(
        UUID id,
        String username,
        String displayName,
        String avatarUrl
) {
}
