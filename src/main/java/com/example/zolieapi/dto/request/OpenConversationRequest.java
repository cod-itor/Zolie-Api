package com.example.zolieapi.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record OpenConversationRequest(
        @NotNull UUID userId
) {
}
