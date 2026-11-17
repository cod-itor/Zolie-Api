package com.example.zolieapi.dto.response;

import java.time.Instant;
import java.util.UUID;

public record ReadReceiptResponse(
        UUID conversationId,
        UUID readerId,
        Instant readUpTo
) {
}
