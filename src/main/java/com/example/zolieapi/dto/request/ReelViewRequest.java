package com.example.zolieapi.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ReelViewRequest(
        @NotNull UUID reelId,
        @Min(0) long watchedMs
) {
}
