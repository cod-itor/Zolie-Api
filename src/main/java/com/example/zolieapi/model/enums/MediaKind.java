package com.example.zolieapi.model.enums;

import com.example.zolieapi.common.LowercaseEnumConverter;
import jakarta.persistence.Converter;

public enum MediaKind {
    IMAGE,
    VIDEO,
    AUDIO;

    @Converter(autoApply = true)
    public static class JpaConverter extends LowercaseEnumConverter<MediaKind> {
        public JpaConverter() {
            super(MediaKind.class);
        }
    }
}
