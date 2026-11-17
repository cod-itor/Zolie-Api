package com.example.zolieapi.model.enums;

import com.example.zolieapi.common.LowercaseEnumConverter;
import jakarta.persistence.Converter;

public enum ReportStatus {
    OPEN,
    REVIEWING,
    RESOLVED,
    DISMISSED;

    @Converter(autoApply = true)
    public static class JpaConverter extends LowercaseEnumConverter<ReportStatus> {
        public JpaConverter() {
            super(ReportStatus.class);
        }
    }
}
