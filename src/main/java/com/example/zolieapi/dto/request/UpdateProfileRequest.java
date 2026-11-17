package com.example.zolieapi.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;

public record UpdateProfileRequest(
        String username,
        @Size(max = 50) String displayName,
        String bio,
        UUID avatarMediaId,
        @Schema(description = "Not accepted. The date of birth is locked after sign-up.") LocalDate dateOfBirth
) {
}
