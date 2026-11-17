package com.example.zolieapi.model.enums;

import com.example.zolieapi.common.LowercaseEnumConverter;
import jakarta.persistence.Converter;

public enum NotificationTarget {
    USER,
    POST,
    REEL,
    COMMENT,
    STORY,
    STORY_STICKER,
    MATCH,
    CONVERSATION,
    REPORT;

    @Converter(autoApply = true)
    public static class JpaConverter extends LowercaseEnumConverter<NotificationTarget> {
        public JpaConverter() {
            super(NotificationTarget.class);
        }
    }
}
