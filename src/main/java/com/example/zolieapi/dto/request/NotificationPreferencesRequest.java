package com.example.zolieapi.dto.request;

public record NotificationPreferencesRequest(
        Boolean follows,
        Boolean likes,
        Boolean comments,
        Boolean mentions,
        Boolean matches,
        Boolean messages,
        Boolean storyInteractions
) {
}
