package com.example.zolieapi.dto.response;

import java.time.Instant;

public record NoteResponse(
        UserSummaryResponse user,
        String text,
        boolean isMine,
        Instant createdAt,
        Instant expiresAt
) {
}
