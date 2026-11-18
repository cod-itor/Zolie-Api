package com.example.zolieapi.model.enums;

import com.example.zolieapi.common.LowercaseEnumConverter;
import jakarta.persistence.Converter;

public enum RsvpStatus {
    INTERESTED,
    GOING;

    @Converter(autoApply = true)
    public static class JpaConverter extends LowercaseEnumConverter<RsvpStatus> {
        public JpaConverter() {
            super(RsvpStatus.class);
        }
    }
}
