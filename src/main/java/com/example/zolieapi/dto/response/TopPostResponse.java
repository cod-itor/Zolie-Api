package com.example.zolieapi.dto.response;

import java.util.UUID;

public record TopPostResponse(
        UUID postId,
        String type,
        String caption,
        long reach,
        int likeCount,
        int commentCount
) {
}
