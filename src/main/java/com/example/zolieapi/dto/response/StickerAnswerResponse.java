package com.example.zolieapi.dto.response;

import java.time.Instant;

public record StickerAnswerResponse(
        UserSummaryResponse user,
        String text,
        Integer value,
        Instant createdAt
) {
}
