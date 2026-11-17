package com.example.zolieapi.service.impl;

import com.example.zolieapi.common.Cursor;
import com.example.zolieapi.common.CursorPage;
import com.example.zolieapi.dto.response.NotificationResponse;
import com.example.zolieapi.dto.response.UnreadCountResponse;
import com.example.zolieapi.exception.ApiException;
import com.example.zolieapi.mapper.NotificationMapper;
import com.example.zolieapi.model.Notification;
import com.example.zolieapi.model.NotificationPreference;
import com.example.zolieapi.model.enums.NotificationTarget;
import com.example.zolieapi.model.enums.NotificationType;
import com.example.zolieapi.repository.BlockRepository;
import com.example.zolieapi.repository.NotificationPreferenceRepository;
import com.example.zolieapi.repository.NotificationRepository;
import com.example.zolieapi.service.NotificationService;
import com.example.zolieapi.service.event.NotificationCreatedEvent;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationPreferenceRepository preferenceRepository;
    private final BlockRepository blockRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final NotificationMapper notificationMapper;

    @Override
    @Transactional
    public void send(UUID recipientId, UUID actorId, NotificationType type, NotificationTarget targetType,
                     UUID targetId) {
        if (recipientId.equals(actorId)) {
            return;
        }
        if (actorId != null && blockRepository.existsBetween(recipientId, actorId)) {
            return;
        }
        Notification notification = notificationRepository.save(Notification.builder()
                .recipientId(recipientId)
                .actorId(actorId)
                .type(type)
                .targetType(targetType)
                .targetId(targetId)
                .build());
        eventPublisher.publishEvent(new NotificationCreatedEvent(notification, isPushAllowed(recipientId, type)));
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPage<NotificationResponse> list(UUID userId, String cursor, Integer limit) {
        int size = CursorPage.limit(limit);
        Cursor from = Cursor.decode(cursor);
        return CursorPage.of(notificationRepository.findPage(userId, from.time(), from.id(), size + 1), size,
                notification -> new Cursor(notification.getCreatedAt(), notification.getId()),
                notificationMapper::toResponses);
    }

    @Override
    @Transactional(readOnly = true)
    public UnreadCountResponse unreadCount(UUID userId) {
        return new UnreadCountResponse(notificationRepository.countByRecipientIdAndReadAtIsNull(userId));
    }

    @Override
    @Transactional
    public void markRead(UUID userId, UUID upToId) {
        Instant upTo = upToId == null
                ? Instant.now()
                : notificationRepository.findById(upToId)
                        .filter(notification -> notification.getRecipientId().equals(userId))
                        .map(Notification::getCreatedAt)
                        .orElseThrow(ApiException::notFound);
        notificationRepository.markReadUpTo(userId, upTo);
    }

    private boolean isPushAllowed(UUID recipientId, NotificationType type) {
        NotificationPreference preference = preferenceRepository.findById(recipientId).orElse(null);
        if (preference == null) {
            return true;
        }
        return switch (type) {
            case FOLLOW, FOLLOW_REQUEST, FOLLOW_ACCEPTED -> preference.isFollows();
            case POST_LIKE, REEL_LIKE -> preference.isLikes();
            case COMMENT -> preference.isComments();
            case MENTION -> preference.isMentions();
            case MATCH, DATING_INTRO -> preference.isMatches();
            case MESSAGE -> preference.isMessages();
            case STORY_REPLY, STICKER_RESPONSE, COUNTDOWN_ENDED -> preference.isStoryInteractions();
            case REPORT_RESOLVED, MODERATION_NOTICE -> true;
        };
    }
}
