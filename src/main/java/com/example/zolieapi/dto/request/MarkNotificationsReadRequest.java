package com.example.zolieapi.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

public record MarkNotificationsReadRequest(
        @Schema(description = "Marks this notification and everything older as read. Leave empty to mark all.") UUID upToId
) {
}
