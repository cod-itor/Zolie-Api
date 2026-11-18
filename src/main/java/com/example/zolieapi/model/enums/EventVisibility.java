package com.example.zolieapi.model.enums;

import com.example.zolieapi.common.LowercaseEnumConverter;
import jakarta.persistence.Converter;

public enum EventVisibility {
    PUBLIC,
    FOLLOWERS;

    @Converter(autoApply = true)
    public static class JpaConverter extends LowercaseEnumConverter<EventVisibility> {
        public JpaConverter() {
            super(EventVisibility.class);
        }
    }
}
