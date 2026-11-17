package com.example.zolieapi.service.impl;

import com.example.zolieapi.common.Cursor;
import com.example.zolieapi.common.CursorPage;
import com.example.zolieapi.common.Enums;
import com.example.zolieapi.common.Texts;
import com.example.zolieapi.dto.request.CreateReelRequest;
import com.example.zolieapi.dto.request.ReelViewRequest;
import com.example.zolieapi.dto.response.LikeResponse;
import com.example.zolieapi.dto.response.ReelResponse;
import com.example.zolieapi.dto.response.SaveResponse;
import com.example.zolieapi.dto.response.ShareResponse;
import com.example.zolieapi.exception.ApiException;
import com.example.zolieapi.mapper.ReelMapper;
import com.example.zolieapi.model.MediaAsset;
import com.example.zolieapi.model.Reel;
import com.example.zolieapi.model.enums.MediaPurpose;
import com.example.zolieapi.model.enums.NotificationTarget;
import com.example.zolieapi.model.enums.NotificationType;
import com.example.zolieapi.model.enums.ReelStatus;
import com.example.zolieapi.model.enums.Visibility;
import com.example.zolieapi.repository.ReelRepository;
import com.example.zolieapi.security.RateLimiter;
import com.example.zolieapi.service.IdempotencyService;
import com.example.zolieapi.service.MediaService;
import com.example.zolieapi.service.MentionService;
import com.example.zolieapi.service.NotificationService;
import com.example.zolieapi.service.ReelService;
import com.example.zolieapi.service.VisibilityService;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReelServiceImpl implements ReelService {

    private static final String RESOURCE = "reel";
    private static final String FOLLOWING_TAB = "following";
    private static final int MAX_CAPTION = 2200;
    private static final int MAX_REEL_MS = 90_000;
    private static final int MAX_VIEWS_PER_BATCH = 50;

    private final ReelRepository reelRepository;
    private final MediaService mediaService;
    private final MentionService mentionService;
    private final NotificationService notificationService;
    private final VisibilityService visibilityService;
    private final IdempotencyService idempotencyService;
    private final RateLimiter rateLimiter;
    private final ReelMapper reelMapper;

    @Override
    @Transactional
    public ReelResponse create(UUID userId, UUID idempotencyKey, CreateReelRequest request) {
        Optional<UUID> existing = idempotencyService.find(userId, idempotencyKey, RESOURCE);
        if (existing.isPresent()) {
            return get(userId, existing.get());
        }
        rateLimiter.check("reel", userId, 20, Duration.ofHours(1));

        MediaAsset video = mediaService.requireUsable(userId, request.videoMediaId(), MediaPurpose.REEL);
        if (video.getDurationMs() == null || video.getDurationMs() > MAX_REEL_MS) {
            throw ApiException.rule("Reels can be at most 90 seconds long.");
        }
        String caption = Texts.clean(request.caption(), MAX_CAPTION, "caption");
        Reel reel = reelRepository.save(Reel.builder()
                .authorId(userId)
                .videoId(video.getId())
                .caption(caption)
                .durationMs(video.getDurationMs())
                .visibility(request.visibility() == null
                        ? Visibility.PUBLIC
                        : Enums.parse(Visibility.class, request.visibility(), "visibility"))
                .build());
        mentionService.mention(userId, caption, null, NotificationTarget.REEL, reel.getId());
        idempotencyService.remember(userId, idempotencyKey, RESOURCE, reel.getId());
        return reelMapper.toResponse(userId, reel);
    }

    @Override
    @Transactional(readOnly = true)
    public ReelResponse get(UUID viewerId, UUID reelId) {
        return reelMapper.toResponse(viewerId, requireVisible(viewerId, reelId));
    }

    @Override
    @Transactional
    public void delete(UUID userId, UUID reelId) {
        Reel reel = requireVisible(userId, reelId);
        if (!reel.getAuthorId().equals(userId)) {
            throw ApiException.forbidden();
        }
        reelRepository.delete(reel);
        reelRepository.flush();
        mediaService.deleteAll(Arrays.asList(reel.getVideoId(), reel.getThumbnailId()));
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPage<ReelResponse> feed(UUID viewerId, String tab, String cursor, Integer limit) {
        int size = CursorPage.limit(limit);
        Cursor from = Cursor.decode(cursor);
        List<Reel> reels = FOLLOWING_TAB.equalsIgnoreCase(tab)
                ? reelRepository.findFollowingFeed(viewerId, from.time(), from.id(), size + 1)
                : reelRepository.findFeed(viewerId, from.time(), from.id(), size + 1);
        return page(viewerId, reels, size);
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPage<ReelResponse> listByAuthor(UUID viewerId, UUID authorId, String cursor, Integer limit) {
        visibilityService.requireCanSeeContentOf(viewerId, authorId);
        int size = CursorPage.limit(limit);
        Cursor from = Cursor.decode(cursor);
        return page(viewerId, reelRepository.findByAuthor(authorId, viewerId, from.time(), from.id(), size + 1), size);
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPage<ReelResponse> listSaved(UUID userId, String cursor, Integer limit) {
        int size = CursorPage.limit(limit);
        Cursor from = Cursor.decode(cursor);
        return page(userId, reelRepository.findSaved(userId, from.time(), from.id(), size + 1), size);
    }

    @Override
    @Transactional
    public LikeResponse like(UUID userId, UUID reelId) {
        rateLimiter.check("like", userId, 120, Duration.ofMinutes(1));
        Reel reel = requireReady(userId, reelId);
        if (reelRepository.insertLike(reelId, userId) == 1) {
            reelRepository.addLikeCount(reelId, 1);
            notificationService.send(reel.getAuthorId(), userId, NotificationType.REEL_LIKE, NotificationTarget.REEL, reelId);
        }
        return new LikeResponse(true, reelRepository.findLikeCount(reelId));
    }

    @Override
    @Transactional
    public LikeResponse unlike(UUID userId, UUID reelId) {
        requireVisible(userId, reelId);
        if (reelRepository.deleteLike(reelId, userId) == 1) {
            reelRepository.addLikeCount(reelId, -1);
        }
        return new LikeResponse(false, reelRepository.findLikeCount(reelId));
    }

    @Override
    @Transactional
    public SaveResponse save(UUID userId, UUID reelId) {
        requireReady(userId, reelId);
        reelRepository.insertSave(reelId, userId);
        return new SaveResponse(true);
    }

    @Override
    @Transactional
    public SaveResponse unsave(UUID userId, UUID reelId) {
        reelRepository.deleteSave(reelId, userId);
        return new SaveResponse(false);
    }

    @Override
    @Transactional
    public ShareResponse share(UUID userId, UUID reelId) {
        rateLimiter.check("share", userId, 60, Duration.ofMinutes(1));
        requireReady(userId, reelId);
        reelRepository.addShare(reelId);
        return new ShareResponse(reelRepository.findShareCount(reelId));
    }

    @Override
    @Transactional
    public void recordViews(UUID viewerId, List<ReelViewRequest> views) {
        views.stream()
                .map(ReelViewRequest::reelId)
                .distinct()
                .limit(MAX_VIEWS_PER_BATCH)
                .filter(reelId -> reelRepository.findVisible(reelId, viewerId)
                        .filter(reel -> reel.getStatus() == ReelStatus.READY)
                        .isPresent())
                .forEach(reelRepository::addView);
    }

    private Reel requireVisible(UUID viewerId, UUID reelId) {
        return reelRepository.findVisible(reelId, viewerId).orElseThrow(ApiException::notFound);
    }

    private Reel requireReady(UUID viewerId, UUID reelId) {
        Reel reel = requireVisible(viewerId, reelId);
        if (reel.getStatus() != ReelStatus.READY) {
            throw ApiException.rule("This Reel is still processing.");
        }
        return reel;
    }

    private CursorPage<ReelResponse> page(UUID viewerId, List<Reel> reels, int size) {
        return CursorPage.of(reels, size, reel -> new Cursor(reel.getCreatedAt(), reel.getId()),
                page -> reelMapper.toResponses(viewerId, page));
    }
}
