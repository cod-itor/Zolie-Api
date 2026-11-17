package com.example.zolieapi.service;

import com.example.zolieapi.model.enums.NotificationTarget;
import com.example.zolieapi.model.enums.NotificationType;
import java.util.UUID;

public interface NotificationService {

    void send(UUID recipientId, UUID actorId, NotificationType type, NotificationTarget targetType, UUID targetId);
}
