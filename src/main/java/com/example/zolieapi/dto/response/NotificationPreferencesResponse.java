package com.example.zolieapi.dto.response;

import java.time.Instant;

public record NotificationPreferencesResponse(
        boolean follows,
        boolean likes,
        boolean comments,
        boolean mentions,
        boolean matches,
        boolean messages,
        boolean storyInteractions,
        boolean events,
        Instant pausedUntil
) {
}
