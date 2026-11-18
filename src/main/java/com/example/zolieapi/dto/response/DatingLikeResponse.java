package com.example.zolieapi.dto.response;

import java.util.UUID;

public record DatingLikeResponse(
        boolean matched,
        UUID matchId,
        UUID conversationId
) {
}
