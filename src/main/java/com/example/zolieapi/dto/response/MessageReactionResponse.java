package com.example.zolieapi.dto.response;

import java.util.UUID;

public record MessageReactionResponse(
        UUID userId,
        String emoji
) {
}
