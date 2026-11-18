package com.example.zolieapi.service;

import com.example.zolieapi.common.CursorPage;
import com.example.zolieapi.dto.request.SendMessageRequest;
import com.example.zolieapi.dto.response.ConversationResponse;
import com.example.zolieapi.dto.response.MessageResponse;
import com.example.zolieapi.dto.response.ReadReceiptResponse;
import com.example.zolieapi.model.Message;
import com.example.zolieapi.model.enums.MessageType;
import java.util.UUID;

public interface ConversationService {

    CursorPage<ConversationResponse> list(UUID userId, String folder, String query, String cursor, Integer limit);

    ConversationResponse open(UUID userId, UUID otherUserId);

    CursorPage<MessageResponse> listMessages(UUID userId, UUID conversationId, String before, Integer limit);

    MessageResponse send(UUID userId, UUID conversationId, SendMessageRequest request);

    ReadReceiptResponse markRead(UUID userId, UUID conversationId, UUID lastReadMessageId);

    void accept(UUID userId, UUID conversationId);

    void clear(UUID userId, UUID conversationId);

    void setMuted(UUID userId, UUID conversationId, boolean muted);

    CursorPage<MessageResponse> listMedia(UUID userId, UUID conversationId, String cursor, Integer limit);

    MessageResponse react(UUID userId, UUID messageId, String emoji);

    MessageResponse removeReaction(UUID userId, UUID messageId);

    UUID otherParticipant(UUID userId, UUID conversationId);

    Message deliverDirect(UUID senderId, UUID recipientId, MessageType type, String text, UUID storyId);

    UUID openDating(UUID matchId, UUID firstUserId, UUID secondUserId);

    void addIntro(UUID conversationId, UUID senderId, String text);
}
