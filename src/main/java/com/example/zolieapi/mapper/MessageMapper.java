package com.example.zolieapi.mapper;

import com.example.zolieapi.common.Enums;
import com.example.zolieapi.dto.response.MediaItemResponse;
import com.example.zolieapi.dto.response.MessageReactionResponse;
import com.example.zolieapi.dto.response.MessageResponse;
import com.example.zolieapi.model.Message;
import com.example.zolieapi.repository.MessageReactionRepository;
import com.example.zolieapi.repository.MessageReactionRepository.ReactionRow;
import com.example.zolieapi.service.MediaService;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MessageMapper {

    private final MediaService mediaService;
    private final MessageReactionRepository reactionRepository;

    public MessageResponse toResponse(Message message) {
        return toResponses(List.of(message)).get(0);
    }

    public List<MessageResponse> toResponses(List<Message> messages) {
        if (messages.isEmpty()) {
            return List.of();
        }
        Map<UUID, MediaItemResponse> media = mediaService.items(messages.stream().map(Message::getMediaId).toList());
        Map<UUID, List<MessageReactionResponse>> reactions = reactionRepository
                .findForMessages(messages.stream().map(Message::getId).toList()).stream()
                .collect(Collectors.groupingBy(ReactionRow::getMessageId, Collectors.mapping(
                        row -> new MessageReactionResponse(row.getUserId(), row.getEmoji()), Collectors.toList())));
        return messages.stream()
                .map(message -> new MessageResponse(
                        message.getId(),
                        message.getConversationId(),
                        message.getSenderId(),
                        message.getClientMessageId(),
                        Enums.value(message.getType()),
                        message.getText(),
                        media.get(message.getMediaId()),
                        message.getReplyToStoryId(),
                        message.getSharedPostId(),
                        message.getSharedReelId(),
                        reactions.getOrDefault(message.getId(), List.of()),
                        message.getCreatedAt()))
                .toList();
    }
}
