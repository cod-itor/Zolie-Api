package com.example.zolieapi.model.enums;

import com.example.zolieapi.common.LowercaseEnumConverter;
import jakarta.persistence.Converter;

public enum MessageType {
    TEXT,
    IMAGE,
    VOICE,
    INTRO,
    STORY_REPLY,
    POST_SHARE,
    REEL_SHARE,
    SYSTEM;

    @Converter(autoApply = true)
    public static class JpaConverter extends LowercaseEnumConverter<MessageType> {
        public JpaConverter() {
            super(MessageType.class);
        }
    }
}
