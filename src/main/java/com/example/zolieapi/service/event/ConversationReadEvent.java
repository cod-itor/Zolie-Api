package com.example.zolieapi.service.event;

import java.time.Instant;
import java.util.UUID;

public record ConversationReadEvent(UUID conversationId, UUID readerId, UUID otherUserId, Instant readUpTo) {
}
