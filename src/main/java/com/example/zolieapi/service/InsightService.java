package com.example.zolieapi.service;

import com.example.zolieapi.dto.response.InsightsResponse;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface InsightService {

    InsightsResponse getMine(UUID userId, Integer days);

    void recordPostViews(UUID viewerId, List<UUID> postIds);

    Map<UUID, Long> reachByPost(Collection<UUID> postIds);
}
