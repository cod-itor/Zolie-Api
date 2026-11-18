package com.example.zolieapi.dto.response;

public record DailyInsightResponse(
        String date,
        long reach,
        long profileVisits,
        long newFollowers
) {
}
