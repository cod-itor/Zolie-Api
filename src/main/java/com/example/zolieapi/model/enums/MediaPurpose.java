package com.example.zolieapi.model.enums;

import com.example.zolieapi.common.LowercaseEnumConverter;
import jakarta.persistence.Converter;

public enum MediaPurpose {
    PROFILE,
    POST,
    STORY,
    REEL,
    REEL_THUMBNAIL,
    CHAT,
    DATING;

    @Converter(autoApply = true)
    public static class JpaConverter extends LowercaseEnumConverter<MediaPurpose> {
        public JpaConverter() {
            super(MediaPurpose.class);
        }
    }
}
