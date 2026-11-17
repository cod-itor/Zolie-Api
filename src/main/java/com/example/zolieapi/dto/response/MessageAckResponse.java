package com.example.zolieapi.dto.response;

import java.time.Instant;
import java.util.UUID;

public record MessageAckResponse(
        UUID clientMessageId,
        UUID messageId,
        Instant createdAt
) {
}
