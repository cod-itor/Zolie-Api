package com.example.zolieapi.dto.response;

import java.util.List;

public record InsightsResponse(
        int days,
        long accountsReached,
        long profileVisits,
        long newFollowers,
        List<DailyInsightResponse> daily,
        List<TopPostResponse> topPosts,
        List<AudiencePlaceResponse> audience
) {
}
