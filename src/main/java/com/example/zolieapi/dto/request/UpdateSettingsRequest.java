package com.example.zolieapi.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

public record UpdateSettingsRequest(
        Boolean isPrivate,
        Boolean showOnlineStatus,
        @Schema(allowableValues = {"system", "light", "dark"}) String theme,
        @Schema(allowableValues = {"km", "en"}) String locale
) {
}
