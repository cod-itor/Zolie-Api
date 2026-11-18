package com.example.zolieapi.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record DatingActionRequest(
        @NotNull UUID toUserId
) {
}
