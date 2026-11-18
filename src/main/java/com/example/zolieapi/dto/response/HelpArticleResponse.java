package com.example.zolieapi.dto.response;

public record HelpArticleResponse(
        int id,
        String topic,
        String titleEn,
        String titleKm,
        String bodyEn,
        String bodyKm,
        boolean popular
) {
}
