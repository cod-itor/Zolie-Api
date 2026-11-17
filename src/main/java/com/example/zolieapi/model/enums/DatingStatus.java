package com.example.zolieapi.model.enums;

import com.example.zolieapi.common.LowercaseEnumConverter;
import jakarta.persistence.Converter;

public enum DatingStatus {
    ACTIVE,
    PAUSED,
    OFF;

    @Converter(autoApply = true)
    public static class JpaConverter extends LowercaseEnumConverter<DatingStatus> {
        public JpaConverter() {
            super(DatingStatus.class);
        }
    }
}
