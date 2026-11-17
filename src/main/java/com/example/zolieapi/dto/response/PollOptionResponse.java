package com.example.zolieapi.dto.response;

import java.util.UUID;

public record PollOptionResponse(
        UUID id,
        String text,
        int voteCount,
        int percent
) {
}
