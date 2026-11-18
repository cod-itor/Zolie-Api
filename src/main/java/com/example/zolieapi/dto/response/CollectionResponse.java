package com.example.zolieapi.dto.response;

import java.time.Instant;
import java.util.UUID;

public record CollectionResponse(
        UUID id,
        String name,
        long itemCount,
        Instant createdAt
) {
}
