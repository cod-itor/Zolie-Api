package com.example.zolieapi.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

public record EventRequest(
        @NotBlank @Size(max = 120) @Schema(example = "Boat races at Riverside") String title,
        @Size(max = 2000) String description,
        @Schema(allowableValues = {"festival", "music", "food", "sports", "market", "community", "other"}) String category,
        @Schema(description = "Uploaded image (purpose event).") UUID coverMediaId,
        @NotNull @Schema(example = "2026-11-24T07:00:00Z") Instant startsAt,
        Instant endsAt,
        UUID placeId,
        @Size(max = 120) @Schema(example = "Sisowath Quay") String venueName,
        Boolean isFree,
        @Size(max = 80) String priceNote,
        @Schema(allowableValues = {"public", "followers"}) String visibility,
        Boolean showGuestList
) {
}
