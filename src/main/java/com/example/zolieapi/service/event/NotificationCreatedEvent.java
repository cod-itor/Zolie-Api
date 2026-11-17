package com.example.zolieapi.service.event;

import com.example.zolieapi.model.Notification;

public record NotificationCreatedEvent(Notification notification, boolean pushAllowed) {
}
