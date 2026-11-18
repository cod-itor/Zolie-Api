package com.example.zolieapi.service.impl;

import com.example.zolieapi.dto.response.AudiencePlaceResponse;
import com.example.zolieapi.dto.response.DailyInsightResponse;
import com.example.zolieapi.dto.response.InsightsResponse;
import com.example.zolieapi.dto.response.TopPostResponse;
import com.example.zolieapi.repository.InsightRepository;
import com.example.zolieapi.repository.InsightRepository.ReachRow;
import com.example.zolieapi.repository.PostRepository;
import com.example.zolieapi.security.RateLimiter;
import com.example.zolieapi.service.InsightService;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InsightServiceImpl implements InsightService {

    private static final int DEFAULT_DAYS = 7;
    private static final int MAX_DAYS = 90;
    private static final int MAX_VIEWS_PER_BATCH = 50;

    private final InsightRepository insightRepository;
    private final PostRepository postRepository;
    private final RateLimiter rateLimiter;

    @Override
    @Transactional(readOnly = true)
    public InsightsResponse getMine(UUID userId, Integer days) {
        int period = days == null ? DEFAULT_DAYS : Math.max(1, Math.min(days, MAX_DAYS));
        List<DailyInsightResponse> daily = insightRepository.findDaily(userId, period).stream()
                .map(row -> new DailyInsightResponse(row.getDay(), row.getReach(), row.getProfileVisits(),
                        row.getNewFollowers()))
                .toList();
        List<TopPostResponse> topPosts = insightRepository.findTopPosts(userId, period).stream()
                .map(row -> new TopPostResponse(row.getPostId(), row.getType(), row.getCaption(), row.getReach(),
                        row.getLikeCount(), row.getCommentCount()))
                .toList();
        List<AudiencePlaceResponse> audience = insightRepository.findAudience(userId, period).stream()
                .map(row -> new AudiencePlaceResponse(row.getCity(), row.getViewers()))
                .toList();
        return new InsightsResponse(period,
                insightRepository.countAccountsReached(userId, period),
                insightRepository.countProfileVisits(userId, period),
                insightRepository.countNewFollowers(userId, period),
                daily, topPosts, audience);
    }

    @Override
    @Transactional
    public void recordPostViews(UUID viewerId, List<UUID> postIds) {
        rateLimiter.check("post-views", viewerId, 60, Duration.ofMinutes(1));
        postIds.stream()
                .distinct()
                .limit(MAX_VIEWS_PER_BATCH)
                .map(postId -> postRepository.findVisible(postId, viewerId))
                .flatMap(Optional::stream)
                .filter(post -> !post.getAuthorId().equals(viewerId))
                .forEach(post -> insightRepository.recordPostView(post.getId(), viewerId));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, Long> reachByPost(Collection<UUID> postIds) {
        if (postIds.isEmpty()) {
            return Map.of();
        }
        return insightRepository.countReach(postIds).stream()
                .collect(Collectors.toMap(ReachRow::getPostId, ReachRow::getReach));
    }
}
