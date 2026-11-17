package com.example.zolieapi.dto.response;

import java.util.UUID;

public record TypingResponse(
        UUID conversationId,
        UUID userId,
        boolean typing
) {
}
