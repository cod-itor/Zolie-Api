package com.example.zolieapi.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

public record NotificationPreferencesRequest(
        Boolean follows,
        Boolean likes,
        Boolean comments,
        Boolean mentions,
        Boolean matches,
        Boolean messages,
        Boolean storyInteractions,
        Boolean events,
        @Schema(description = "Pause all push until this time. Send a past time to resume.") Instant pausedUntil
) {
}
