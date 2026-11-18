package com.example.zolieapi.dto.response;

import java.util.UUID;

public record PromptAnswerResponse(
        UUID id,
        int promptId,
        String promptEn,
        String promptKm,
        String answer
) {
}
