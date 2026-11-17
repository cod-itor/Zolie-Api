package com.example.zolieapi.dto.response;

public record NotificationPreferencesResponse(
        boolean follows,
        boolean likes,
        boolean comments,
        boolean mentions,
        boolean matches,
        boolean messages,
        boolean storyInteractions
) {
}
