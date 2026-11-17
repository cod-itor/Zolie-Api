package com.example.zolieapi.model.enums;

import com.example.zolieapi.common.LowercaseEnumConverter;
import jakarta.persistence.Converter;

public enum ConversationType {
    REGULAR,
    DATING;

    @Converter(autoApply = true)
    public static class JpaConverter extends LowercaseEnumConverter<ConversationType> {
        public JpaConverter() {
            super(ConversationType.class);
        }
    }
}
