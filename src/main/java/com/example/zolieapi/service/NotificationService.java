package com.example.zolieapi.service;

import com.example.zolieapi.model.enums.NotificationTarget;
import com.example.zolieapi.model.enums.NotificationType;
import com.example.zolieapi.common.CursorPage;
import com.example.zolieapi.dto.response.NotificationResponse;
import com.example.zolieapi.dto.response.UnreadCountResponse;
import java.util.UUID;

public interface NotificationService {

    void send(UUID recipientId, UUID actorId, NotificationType type, NotificationTarget targetType, UUID targetId);

    CursorPage<NotificationResponse> list(UUID userId, String cursor, Integer limit);

    UnreadCountResponse unreadCount(UUID userId);

    void markRead(UUID userId, UUID upToId);
}
