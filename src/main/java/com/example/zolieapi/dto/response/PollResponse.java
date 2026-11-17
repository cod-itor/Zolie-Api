package com.example.zolieapi.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PollResponse(
        List<PollOptionResponse> options,
        int totalVotes,
        Instant endsAt,
        boolean ended,
        UUID votedOptionId
) {
}
