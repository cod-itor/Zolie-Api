package com.example.zolieapi.dto.response;

import java.time.Instant;

public record DeletionResponse(
        Instant deleteAfter
) {
}
