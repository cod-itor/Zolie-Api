package com.example.zolieapi.dto.request;

import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

public record UpdateEventRequest(
        @Size(max = 120) String title,
        @Size(max = 2000) String description,
        String category,
        UUID coverMediaId,
        Instant startsAt,
        Instant endsAt,
        UUID placeId,
        @Size(max = 120) String venueName,
        Boolean isFree,
        @Size(max = 80) String priceNote,
        String visibility,
        Boolean showGuestList
) {
}
