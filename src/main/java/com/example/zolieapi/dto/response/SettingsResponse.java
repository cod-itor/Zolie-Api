package com.example.zolieapi.dto.response;

public record SettingsResponse(
        boolean isPrivate,
        boolean showOnlineStatus,
        String theme,
        String locale
) {
}
