package com.example.zolieapi.model.enums;

import com.example.zolieapi.common.LowercaseEnumConverter;
import jakarta.persistence.Converter;

public enum NotificationType {
    FOLLOW,
    FOLLOW_REQUEST,
    FOLLOW_ACCEPTED,
    POST_LIKE,
    REEL_LIKE,
    COMMENT,
    COMMENT_LIKE,
    MENTION,
    MATCH,
    DATING_INTRO,
    MESSAGE,
    STORY_REPLY,
    STICKER_RESPONSE,
    COUNTDOWN_ENDED,
    EVENT_REMINDER,
    REPORT_RESOLVED,
    MODERATION_NOTICE;

    @Converter(autoApply = true)
    public static class JpaConverter extends LowercaseEnumConverter<NotificationType> {
        public JpaConverter() {
            super(NotificationType.class);
        }
    }
}
