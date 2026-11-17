package com.example.zolieapi.service.event;

import com.example.zolieapi.model.Message;
import java.util.UUID;

public record MessageSentEvent(Message message, UUID recipientId, boolean recipientMuted) {
}
