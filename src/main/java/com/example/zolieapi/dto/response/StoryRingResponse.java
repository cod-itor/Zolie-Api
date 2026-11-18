package com.example.zolieapi.dto.response;

public record StoryRingResponse(
        UserSummaryResponse user,
        boolean hasUnseen,
        boolean isCloseFriends,
        long storyCount
) {
}
