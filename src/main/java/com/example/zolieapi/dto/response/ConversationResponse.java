package com.example.zolieapi.dto.response;

import java.time.Instant;
import java.util.UUID;

public record ConversationResponse(
        UUID id,
        String type,
        UserSummaryResponse otherUser,
        boolean otherUserOnline,
        MessageResponse lastMessage,
        long unreadCount,
        boolean isRequest,
        boolean muted,
        UUID matchId,
        Instant lastMessageAt,
        Instant createdAt
) {
}
