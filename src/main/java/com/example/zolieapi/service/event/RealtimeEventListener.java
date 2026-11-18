package com.example.zolieapi.service.event;

import com.example.zolieapi.common.Enums;
import com.example.zolieapi.common.PushMessages;
import com.example.zolieapi.dto.response.MessageReactionEventResponse;
import com.example.zolieapi.dto.response.MessageResponse;
import com.example.zolieapi.dto.response.NotificationPushResponse;
import com.example.zolieapi.dto.response.NotificationResponse;
import com.example.zolieapi.dto.response.ReadReceiptResponse;
import com.example.zolieapi.mapper.MessageMapper;
import com.example.zolieapi.mapper.NotificationMapper;
import com.example.zolieapi.model.Message;
import com.example.zolieapi.model.Notification;
import com.example.zolieapi.model.NotificationPreference;
import com.example.zolieapi.model.User;
import com.example.zolieapi.model.enums.NotificationType;
import com.example.zolieapi.repository.NotificationPreferenceRepository;
import com.example.zolieapi.repository.NotificationRepository;
import com.example.zolieapi.repository.UserRepository;
import com.example.zolieapi.service.PushService;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class RealtimeEventListener {

    private static final String MESSAGES_QUEUE = "/queue/messages";
    private static final String RECEIPTS_QUEUE = "/queue/receipts";
    private static final String REACTIONS_QUEUE = "/queue/reactions";
    private static final String NOTIFICATIONS_QUEUE = "/queue/notifications";
    private static final String NEW_MESSAGE = "new_message";
    private static final int MAX_PREVIEW = 80;

    private final SimpMessagingTemplate messagingTemplate;
    private final SimpUserRegistry userRegistry;
    private final PushService pushService;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationPreferenceRepository preferenceRepository;
    private final MessageMapper messageMapper;
    private final NotificationMapper notificationMapper;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onMessageSent(MessageSentEvent event) {
        Message message = event.message();
        MessageResponse response = messageMapper.toResponse(message);
        messagingTemplate.convertAndSendToUser(event.recipientId().toString(), MESSAGES_QUEUE, response);
        messagingTemplate.convertAndSendToUser(message.getSenderId().toString(), MESSAGES_QUEUE, response);

        if (isConnected(event.recipientId()) || event.recipientMuted() || !allowsMessagePush(event.recipientId())) {
            return;
        }
        Map<String, String> data = new HashMap<>();
        data.put("type", NEW_MESSAGE);
        data.put("conversationId", message.getConversationId().toString());
        data.put("messageId", message.getId().toString());
        data.put("title", displayName(message.getSenderId()));
        data.put("body", preview(message, event.recipientId()));
        pushService.send(event.recipientId(), data);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMessageReaction(MessageReactionEvent event) {
        MessageReactionEventResponse response = new MessageReactionEventResponse(event.conversationId(),
                event.messageId(), event.userId(), event.emoji());
        messagingTemplate.convertAndSendToUser(event.otherUserId().toString(), REACTIONS_QUEUE, response);
        messagingTemplate.convertAndSendToUser(event.userId().toString(), REACTIONS_QUEUE, response);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onConversationRead(ConversationReadEvent event) {
        messagingTemplate.convertAndSendToUser(event.otherUserId().toString(), RECEIPTS_QUEUE,
                new ReadReceiptResponse(event.conversationId(), event.readerId(), event.readUpTo()));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onNotificationCreated(NotificationCreatedEvent event) {
        Notification notification = event.notification();
        UUID recipientId = notification.getRecipientId();
        NotificationResponse response = notificationMapper.toResponse(notification);
        long unreadCount = notificationRepository.countUnread(recipientId);
        messagingTemplate.convertAndSendToUser(recipientId.toString(), NOTIFICATIONS_QUEUE,
                new NotificationPushResponse(response, unreadCount));

        if (!event.pushAllowed()) {
            return;
        }
        Map<String, String> data = new HashMap<>();
        data.put("type", Enums.value(notification.getType()));
        data.put("notificationId", notification.getId().toString());
        if (notification.getTargetId() != null) {
            data.put("targetType", Enums.value(notification.getTargetType()));
            data.put("targetId", notification.getTargetId().toString());
        }
        data.put("title", notification.getActorId() == null ? "Zolie" : displayName(notification.getActorId()));
        data.put("body", PushMessages.body(notification.getType(), localeOf(recipientId)));
        pushService.send(recipientId, data);
    }

    private boolean isConnected(UUID userId) {
        return userRegistry.getUser(userId.toString()) != null;
    }

    private boolean allowsMessagePush(UUID userId) {
        return preferenceRepository.findById(userId).map(NotificationPreference::isMessages).orElse(true);
    }

    private String displayName(UUID userId) {
        return userRepository.findById(userId).map(User::getDisplayName).orElse("Zolie");
    }

    private String localeOf(UUID userId) {
        return userRepository.findById(userId).map(User::getLocale).orElse("en");
    }

    private String preview(Message message, UUID recipientId) {
        String text = message.getText();
        if (text == null || text.isBlank()) {
            return PushMessages.body(NotificationType.MESSAGE, localeOf(recipientId));
        }
        return text.codePointCount(0, text.length()) <= MAX_PREVIEW
                ? text
                : text.substring(0, text.offsetByCodePoints(0, MAX_PREVIEW)) + "…";
    }
}
