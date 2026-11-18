package com.example.zolieapi.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

public record UpdateSettingsRequest(
        Boolean isPrivate,
        Boolean showOnlineStatus,
        Boolean readReceipts,
        Boolean dataSaver,
        Boolean hideDatingFromFollowing,
        @Schema(allowableValues = {"everyone", "followers", "nobody"}) String whoCanMessage,
        @Schema(allowableValues = {"everyone", "followers", "nobody"}) String whoCanMention,
        @Schema(allowableValues = {"everyone", "followers", "nobody"}) String storyRepliesFrom,
        @Schema(allowableValues = {"system", "light", "dark"}) String theme,
        @Schema(allowableValues = {"km", "en"}) String locale
) {
}
