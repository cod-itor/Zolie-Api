package com.example.zolieapi.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record UpdateProfileRequest(
        String username,
        @Size(max = 50) String displayName,
        @Size(max = 50) String khmerName,
        String bio,
        @Size(max = 200) String websiteUrl,
        @Size(max = 80) String city,
        UUID avatarMediaId,
        UUID coverMediaId,
        @Schema(description = "Replaces Places I love (up to 6).") List<UUID> favoritePlaceIds,
        @Schema(description = "Not accepted. The date of birth is locked after sign-up.") LocalDate dateOfBirth
) {
}
