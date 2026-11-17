package com.example.zolieapi.dto.request;

import java.util.List;
import java.util.UUID;

public record UpdatePostRequest(
        String caption,
        List<UUID> taggedUserIds
) {
}
