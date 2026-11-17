package com.example.zolieapi.model.enums;

import com.example.zolieapi.common.LowercaseEnumConverter;
import jakarta.persistence.Converter;

public enum PostType {
    TEXT,
    PHOTO,
    CAROUSEL,
    VIDEO,
    POLL,
    AUDIO;

    @Converter(autoApply = true)
    public static class JpaConverter extends LowercaseEnumConverter<PostType> {
        public JpaConverter() {
            super(PostType.class);
        }
    }
}
