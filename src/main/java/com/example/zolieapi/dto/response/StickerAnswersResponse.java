package com.example.zolieapi.dto.response;

import java.util.List;

public record StickerAnswersResponse(
        List<StickerAnswerResponse> items,
        String nextCursor,
        int responseCount,
        Double sliderAverage
) {
}
