package com.example.zolieapi.dto.response;

public record SettingsResponse(
        boolean isPrivate,
        boolean showOnlineStatus,
        boolean readReceipts,
        boolean dataSaver,
        boolean hideDatingFromFollowing,
        String whoCanMessage,
        String whoCanMention,
        String storyRepliesFrom,
        String theme,
        String locale
) {
}
