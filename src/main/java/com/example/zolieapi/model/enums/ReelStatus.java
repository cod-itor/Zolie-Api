package com.example.zolieapi.model.enums;

import com.example.zolieapi.common.LowercaseEnumConverter;
import jakarta.persistence.Converter;

public enum ReelStatus {
    PROCESSING,
    READY,
    FAILED;

    @Converter(autoApply = true)
    public static class JpaConverter extends LowercaseEnumConverter<ReelStatus> {
        public JpaConverter() {
            super(ReelStatus.class);
        }
    }
}
