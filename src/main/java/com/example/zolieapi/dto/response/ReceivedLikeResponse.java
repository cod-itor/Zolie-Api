package com.example.zolieapi.dto.response;

import java.time.Instant;

public record ReceivedLikeResponse(
        DatingCardResponse profile,
        String type,
        String introMessage,
        PromptAnswerResponse promptAnswer,
        Instant createdAt
) {
}
