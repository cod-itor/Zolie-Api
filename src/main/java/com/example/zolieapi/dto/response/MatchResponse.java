package com.example.zolieapi.dto.response;

import java.time.Instant;
import java.util.UUID;

public record MatchResponse(
        UUID id,
        UUID conversationId,
        DatingCardResponse profile,
        Instant createdAt
) {
}
