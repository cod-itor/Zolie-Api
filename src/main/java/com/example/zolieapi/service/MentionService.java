package com.example.zolieapi.service;

import com.example.zolieapi.model.enums.NotificationTarget;
import java.util.Collection;
import java.util.UUID;

public interface MentionService {

    void mention(UUID authorId, String text, Collection<UUID> taggedUserIds, NotificationTarget source, UUID sourceId);

    void replaceForPost(UUID authorId, UUID postId, String text, Collection<UUID> taggedUserIds);
}
