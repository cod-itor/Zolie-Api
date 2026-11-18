package com.example.zolieapi.dto.request;

import java.util.UUID;

public record CollectionItemRequest(
        UUID postId,
        UUID reelId
) {
}
