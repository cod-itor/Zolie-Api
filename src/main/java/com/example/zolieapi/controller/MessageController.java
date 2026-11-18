package com.example.zolieapi.controller;

import com.example.zolieapi.dto.request.ReactionRequest;
import com.example.zolieapi.dto.response.MessageResponse;
import com.example.zolieapi.security.CurrentUser;
import com.example.zolieapi.service.ConversationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/messages")
@RequiredArgsConstructor
@Tag(name = "15. Messaging")
public class MessageController {

    private final ConversationService conversationService;
    private final CurrentUser currentUser;

    @PutMapping("/{id}/reaction")
    @Operation(summary = "React to a message with an emoji (one reaction per person)")
    public MessageResponse react(@PathVariable UUID id, @Valid @RequestBody ReactionRequest request) {
        return conversationService.react(currentUser.id(), id, request.reaction());
    }

    @DeleteMapping("/{id}/reaction")
    @Operation(summary = "Remove my reaction from a message")
    public MessageResponse removeReaction(@PathVariable UUID id) {
        return conversationService.removeReaction(currentUser.id(), id);
    }
}
