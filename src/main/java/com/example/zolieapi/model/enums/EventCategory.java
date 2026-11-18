package com.example.zolieapi.model.enums;

import com.example.zolieapi.common.LowercaseEnumConverter;
import jakarta.persistence.Converter;

public enum EventCategory {
    FESTIVAL,
    MUSIC,
    FOOD,
    SPORTS,
    MARKET,
    COMMUNITY,
    OTHER;

    @Converter(autoApply = true)
    public static class JpaConverter extends LowercaseEnumConverter<EventCategory> {
        public JpaConverter() {
            super(EventCategory.class);
        }
    }
}
