package com.example.zolieapi.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

public record ReadRequest(
        @Schema(description = "Leave empty to mark everything as read.") UUID lastReadMessageId
) {
}
