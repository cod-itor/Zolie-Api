package com.example.zolieapi.controller;

import com.example.zolieapi.dto.request.ReadRequest;
import com.example.zolieapi.dto.request.SendMessageRequest;
import com.example.zolieapi.dto.request.TypingRequest;
import com.example.zolieapi.dto.response.MessageAckResponse;
import com.example.zolieapi.dto.response.MessageResponse;
import com.example.zolieapi.dto.response.TypingResponse;
import com.example.zolieapi.exception.ApiException;
import com.example.zolieapi.exception.ErrorResponse;
import com.example.zolieapi.service.ConversationService;
import java.security.Principal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class ChatSocketController {

    private static final String ACKS_QUEUE = "/queue/acks";
    private static final String TYPING_QUEUE = "/queue/typing";

    private final ConversationService conversationService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/conversations/{id}/messages")
    public void send(@DestinationVariable UUID id, @Payload SendMessageRequest request, Principal principal) {
        MessageResponse message = conversationService.send(userId(principal), id, request);
        messagingTemplate.convertAndSendToUser(principal.getName(), ACKS_QUEUE,
                new MessageAckResponse(message.clientMessageId(), message.id(), message.createdAt()));
    }

    @MessageMapping("/conversations/{id}/typing")
    public void typing(@DestinationVariable UUID id, @Payload TypingRequest request, Principal principal) {
        UUID userId = userId(principal);
        UUID otherUserId = conversationService.otherParticipant(userId, id);
        messagingTemplate.convertAndSendToUser(otherUserId.toString(), TYPING_QUEUE,
                new TypingResponse(id, userId, request.typing()));
    }

    @MessageMapping("/conversations/{id}/read")
    public void read(@DestinationVariable UUID id, @Payload ReadRequest request, Principal principal) {
        conversationService.markRead(userId(principal), id, request.lastReadMessageId());
    }

    @MessageExceptionHandler(ApiException.class)
    @SendToUser("/queue/errors")
    public ErrorResponse handleError(ApiException e) {
        return ErrorResponse.of(e.getCode(), e.getMessage());
    }

    private UUID userId(Principal principal) {
        return UUID.fromString(principal.getName());
    }
}
