package com.example.zolieapi.service.impl;

import com.example.zolieapi.model.Story;
import com.example.zolieapi.model.StorySticker;
import com.example.zolieapi.model.User;
import com.example.zolieapi.model.StoryStickerResponse;
import com.example.zolieapi.model.enums.NotificationTarget;
import com.example.zolieapi.model.enums.NotificationType;
import com.example.zolieapi.repository.IdempotencyKeyRepository;
import com.example.zolieapi.repository.MediaAssetRepository;
import com.example.zolieapi.repository.MediaAssetRepository.DeletionRow;
import com.example.zolieapi.repository.StoryRepository;
import com.example.zolieapi.repository.StoryStickerRepository;
import com.example.zolieapi.repository.UserNoteRepository;
import com.example.zolieapi.repository.StoryStickerResponseRepository;
import com.example.zolieapi.repository.UserRepository;
import com.example.zolieapi.service.AccountService;
import com.example.zolieapi.service.EventService;
import com.example.zolieapi.service.MaintenanceService;
import com.example.zolieapi.service.MediaService;
import com.example.zolieapi.service.NotificationService;
import com.example.zolieapi.service.StorageService;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MaintenanceServiceImpl implements MaintenanceService {

    private static final int BATCH_SIZE = 200;
    private static final int DELETION_BATCH = 20;
    private static final int MAX_ERROR_LENGTH = 500;
    private static final Duration IDEMPOTENCY_RETENTION = Duration.ofDays(2);

    private final StoryRepository storyRepository;
    private final UserNoteRepository userNoteRepository;
    private final StoryStickerRepository stickerRepository;
    private final StoryStickerResponseRepository responseRepository;
    private final MediaAssetRepository mediaAssetRepository;
    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private final UserRepository userRepository;
    private final MediaService mediaService;
    private final AccountService accountService;
    private final EventService eventService;
    private final StorageService storageService;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public void expireStories() {
        if (!mediaAssetRepository.tryLockJob("expire-stories")) {
            return;
        }
        List<Story> expired = storyRepository.findExpired(BATCH_SIZE);
        if (!expired.isEmpty()) {
            storyRepository.deleteAllInBatch(expired);
            mediaService.deleteAll(expired.stream().map(Story::getMediaId).toList());
            log.info("Expired {} stories", expired.size());
        }
        userNoteRepository.deleteExpired(Instant.now());
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
    public void sendEventReminders() {
        if (mediaAssetRepository.tryLockJob("event-reminders")) {
            eventService.sendReminders();
        }
    }

    @Override
    public void completeAccountDeletions() {
        for (User user : userRepository.findDueForDeletion(DELETION_BATCH)) {
            try {
                accountService.deleteAccount(user.getId());
                log.info("Deleted account after the grace period: user={}", user.getId());
            } catch (RuntimeException e) {
                log.error("Could not delete account after the grace period: user={}", user.getId(), e);
            }
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
