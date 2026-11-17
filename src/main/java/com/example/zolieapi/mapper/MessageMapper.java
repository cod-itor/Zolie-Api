package com.example.zolieapi.mapper;

import com.example.zolieapi.common.Enums;
import com.example.zolieapi.dto.response.MediaItemResponse;
import com.example.zolieapi.dto.response.MessageResponse;
import com.example.zolieapi.model.Message;
import com.example.zolieapi.service.MediaService;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MessageMapper {

    private final MediaService mediaService;

    public MessageResponse toResponse(Message message) {
        return toResponses(List.of(message)).get(0);
    }

    public List<MessageResponse> toResponses(List<Message> messages) {
        Map<UUID, MediaItemResponse> media = mediaService.items(messages.stream().map(Message::getMediaId).toList());
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
                        message.getCreatedAt()))
                .toList();
    }
}
