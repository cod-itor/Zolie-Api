package com.example.zolieapi.dto.response;

import java.time.Instant;
import java.util.UUID;

public record StickerResponse(
        UUID id,
        String type,
        String prompt,
        String emoji,
        Instant endsAt,
        float x,
        float y,
        float scale,
        float rotation,
        boolean answered,
        boolean reminderSet,
        Integer responseCount
) {
}
