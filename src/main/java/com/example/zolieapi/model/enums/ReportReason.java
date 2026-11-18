package com.example.zolieapi.model.enums;

import com.example.zolieapi.common.LowercaseEnumConverter;
import jakarta.persistence.Converter;

public enum ReportReason {
    SPAM,
    HARASSMENT,
    NUDITY,
    VIOLENCE,
    HATE,
    UNDERAGE,
    SCAM,
    FAKE_PROFILE,
    FALSE_INFO,
    ILLEGAL_GOODS,
    OTHER;

    @Converter(autoApply = true)
    public static class JpaConverter extends LowercaseEnumConverter<ReportReason> {
        public JpaConverter() {
            super(ReportReason.class);
        }
    }
}
