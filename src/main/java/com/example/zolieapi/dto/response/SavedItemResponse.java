package com.example.zolieapi.dto.response;

import java.time.Instant;

public record SavedItemResponse(
        String type,
        PostResponse post,
        ReelResponse reel,
        Instant addedAt
) {
}
