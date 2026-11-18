package com.example.zolieapi.service.event;

import java.util.UUID;

public record MessageReactionEvent(UUID conversationId, UUID messageId, UUID userId, UUID otherUserId, String emoji) {
}
