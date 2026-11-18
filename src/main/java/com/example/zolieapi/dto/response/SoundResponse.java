package com.example.zolieapi.dto.response;

import java.util.UUID;

public record SoundResponse(
        UUID id,
        String title,
        String artistName,
        String audioUrl,
        Integer durationMs,
        boolean isLibrary,
        int useCount
) {
}
