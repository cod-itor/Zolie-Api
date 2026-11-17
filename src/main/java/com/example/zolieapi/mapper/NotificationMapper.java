package com.example.zolieapi.mapper;

import com.example.zolieapi.common.Enums;
import com.example.zolieapi.dto.response.NotificationResponse;
import com.example.zolieapi.dto.response.UserSummaryResponse;
import com.example.zolieapi.model.Notification;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationMapper {

    private final UserMapper userMapper;

    public NotificationResponse toResponse(Notification notification) {
        return toResponses(List.of(notification)).get(0);
    }

    public List<NotificationResponse> toResponses(List<Notification> notifications) {
        Map<UUID, UserSummaryResponse> actors = userMapper.summariesById(notifications.stream()
                .map(Notification::getActorId)
                .filter(Objects::nonNull)
                .toList());
        return notifications.stream()
                .map(notification -> new NotificationResponse(
                        notification.getId(),
                        Enums.value(notification.getType()),
                        actors.get(notification.getActorId()),
                        Enums.value(notification.getTargetType()),
                        notification.getTargetId(),
                        notification.getReadAt() != null,
                        notification.getCreatedAt()))
                .toList();
    }
}
