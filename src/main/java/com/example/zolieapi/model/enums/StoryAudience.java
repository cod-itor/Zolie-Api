package com.example.zolieapi.model.enums;

import com.example.zolieapi.common.LowercaseEnumConverter;
import jakarta.persistence.Converter;

public enum StoryAudience {
    EVERYONE,
    CLOSE_FRIENDS;

    @Converter(autoApply = true)
    public static class JpaConverter extends LowercaseEnumConverter<StoryAudience> {
        public JpaConverter() {
            super(StoryAudience.class);
        }
    }
}
