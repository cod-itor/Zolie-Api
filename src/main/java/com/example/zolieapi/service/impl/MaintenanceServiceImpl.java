package com.example.zolieapi.service.impl;

import com.example.zolieapi.model.StorySticker;
import com.example.zolieapi.model.StoryStickerResponse;
import com.example.zolieapi.model.enums.NotificationTarget;
import com.example.zolieapi.model.enums.NotificationType;
import com.example.zolieapi.repository.IdempotencyKeyRepository;
import com.example.zolieapi.repository.MediaAssetRepository;
import com.example.zolieapi.repository.MediaAssetRepository.DeletionRow;
import com.example.zolieapi.repository.StoryRepository;
import com.example.zolieapi.repository.StoryStickerRepository;
import com.example.zolieapi.repository.StoryStickerResponseRepository;
import com.example.zolieapi.repository.UserRepository;
import com.example.zolieapi.service.MaintenanceService;
import com.example.zolieapi.service.MediaService;
import com.example.zolieapi.service.NotificationService;
import com.example.zolieapi.service.StorageService;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MaintenanceServiceImpl implements MaintenanceService {

    private static final int BATCH_SIZE = 200;
    private static final int MAX_ERROR_LENGTH = 500;
    private static final Duration IDEMPOTENCY_RETENTION = Duration.ofDays(2);

    private final StoryRepository storyRepository;
    private final StoryStickerRepository stickerRepository;
    private final StoryStickerResponseRepository responseRepository;
    private final MediaAssetRepository mediaAssetRepository;
    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private final UserRepository userRepository;
    private final MediaService mediaService;
    private final StorageService storageService;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public void expireStories() {
        if (!mediaAssetRepository.tryLockJob("expire-stories")) {
            return;
        }
        List<UUID> mediaIds = storyRepository.findExpiredMediaIds(BATCH_SIZE);
        if (!mediaIds.isEmpty()) {
            mediaService.deleteAll(mediaIds);
            log.info("Expired {} stories", mediaIds.size());
        }
    }

    @Override
    @Transactional
    public void sendCountdownReminders() {
        if (!mediaAssetRepository.tryLockJob("countdown-reminders")) {
            return;
        }
        for (StorySticker sticker : stickerRepository.findEndedCountdownsWithReminders(BATCH_SIZE)) {
            for (StoryStickerResponse reminder : responseRepository.findByStickerIdAndRemindMeTrue(sticker.getId())) {
                notificationService.send(reminder.getResponderId(), null, NotificationType.COUNTDOWN_ENDED,
                        NotificationTarget.STORY_STICKER, sticker.getId());
            }
            responseRepository.deleteReminders(sticker.getId());
        }
    }

    @Override
    @Transactional
    public void cleanStorage() {
        if (!mediaAssetRepository.tryLockJob("clean-storage")) {
            return;
        }
        for (DeletionRow row : mediaAssetRepository.findQueuedDeletions(BATCH_SIZE)) {
            try {
                storageService.delete(row.getObjectKey());
                mediaAssetRepository.removeQueuedDeletion(row.getId());
            } catch (RuntimeException e) {
                String error = String.valueOf(e.getMessage());
                mediaAssetRepository.markDeletionFailed(row.getId(),
                        error.substring(0, Math.min(error.length(), MAX_ERROR_LENGTH)));
            }
        }
    }

    @Override
    @Transactional
    public void removeAbandonedUploads() {
        if (!mediaAssetRepository.tryLockJob("abandoned-uploads")) {
            return;
        }
        int removed = mediaAssetRepository.deleteAbandonedUploads();
        if (removed > 0) {
            log.info("Removed {} abandoned uploads", removed);
        }
    }

    @Override
    @Transactional
    public void liftEndedSuspensions() {
        if (!mediaAssetRepository.tryLockJob("lift-suspensions")) {
            return;
        }
        int lifted = userRepository.liftEndedSuspensions();
        if (lifted > 0) {
            log.info("Lifted {} suspensions", lifted);
        }
    }

    @Override
    @Transactional
    public void purgeIdempotencyKeys() {
        if (!mediaAssetRepository.tryLockJob("purge-idempotency")) {
            return;
        }
        idempotencyKeyRepository.deleteOlderThan(Instant.now().minus(IDEMPOTENCY_RETENTION));
    }
}
