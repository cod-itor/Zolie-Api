package com.example.zolieapi.dto.response;

import java.time.Instant;
import java.util.UUID;

public record DeviceResponse(
        UUID id,
        String deviceName,
        String platform,
        Instant lastSeenAt
) {
}
