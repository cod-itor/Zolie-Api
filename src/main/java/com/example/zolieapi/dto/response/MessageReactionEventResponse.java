package com.example.zolieapi.dto.response;

import java.util.UUID;

public record MessageReactionEventResponse(
        UUID conversationId,
        UUID messageId,
        UUID userId,
        String emoji
) {
}
