package com.example.zolieapi.model.enums;

import com.example.zolieapi.common.LowercaseEnumConverter;
import jakarta.persistence.Converter;

public enum ReportTarget {
    USER,
    POST,
    REEL,
    COMMENT,
    STORY,
    MESSAGE;

    @Converter(autoApply = true)
    public static class JpaConverter extends LowercaseEnumConverter<ReportTarget> {
        public JpaConverter() {
            super(ReportTarget.class);
        }
    }
}
