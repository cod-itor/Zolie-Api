package com.example.zolieapi.dto.response;

public record LikeResponse(
        boolean liked,
        String reaction,
        long likeCount
) {
}
