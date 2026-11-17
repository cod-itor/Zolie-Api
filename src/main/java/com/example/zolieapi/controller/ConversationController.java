package com.example.zolieapi.controller;

import com.example.zolieapi.common.CursorPage;
import com.example.zolieapi.dto.request.OpenConversationRequest;
import com.example.zolieapi.dto.request.ReadRequest;
import com.example.zolieapi.dto.request.SendMessageRequest;
import com.example.zolieapi.dto.response.ConversationResponse;
import com.example.zolieapi.dto.response.MessageResponse;
import com.example.zolieapi.dto.response.ReadReceiptResponse;
import com.example.zolieapi.security.CurrentUser;
import com.example.zolieapi.service.ConversationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/conversations")
@RequiredArgsConstructor
@Tag(name = "15. Messaging")
public class ConversationController {

    private final ConversationService conversationService;
    private final CurrentUser currentUser;

    @GetMapping
    @Operation(summary = "List my chats with the last message and unread count")
    public CursorPage<ConversationResponse> list(
            @Parameter(schema = @Schema(allowableValues = {"inbox", "requests", "dating"}))
            @RequestParam(defaultValue = "inbox") String folder,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) Integer limit) {
        return conversationService.list(currentUser.id(), folder, cursor, limit);
    }

    @PostMapping
    @Operation(summary = "Find or create the regular chat with someone")
    public ConversationResponse open(@Valid @RequestBody OpenConversationRequest request) {
        return conversationService.open(currentUser.id(), request.userId());
    }

    @GetMapping("/{id}/messages")
    @Operation(summary = "Load messages, newest first; pass nextCursor as 'before' for older ones")
    public CursorPage<MessageResponse> listMessages(@PathVariable UUID id,
                                                    @RequestParam(required = false) String before,
                                                    @RequestParam(required = false) Integer limit) {
        return conversationService.listMessages(currentUser.id(), id, before, limit);
    }

    @PostMapping("/{id}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Send a text, image, shared post or shared Reel")
    public MessageResponse send(@PathVariable UUID id, @Valid @RequestBody SendMessageRequest request) {
        return conversationService.send(currentUser.id(), id, request);
    }

    @PostMapping("/{id}/read")
    @Operation(summary = "Mark the chat as read up to a message")
    public ReadReceiptResponse markRead(@PathVariable UUID id, @RequestBody(required = false) ReadRequest request) {
        UUID lastReadMessageId = request == null ? null : request.lastReadMessageId();
        return conversationService.markRead(currentUser.id(), id, lastReadMessageId);
    }

    @PostMapping("/{id}/accept")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Accept a message request")
    public void accept(@PathVariable UUID id) {
        conversationService.accept(currentUser.id(), id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete the chat for me only")
    public void clear(@PathVariable UUID id) {
        conversationService.clear(currentUser.id(), id);
    }
}
