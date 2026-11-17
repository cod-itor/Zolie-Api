package com.example.zolieapi.dto.response;

public record NotificationPushResponse(
        NotificationResponse notification,
        long unreadCount
) {
}
