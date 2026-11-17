package com.example.zolieapi.model.enums;

import com.example.zolieapi.common.LowercaseEnumConverter;
import jakarta.persistence.Converter;

public enum ModerationActionType {
    REMOVE_CONTENT,
    RESTORE_CONTENT,
    WARN_USER,
    SUSPEND_USER,
    UNSUSPEND_USER,
    DISMISS_REPORT;

    @Converter(autoApply = true)
    public static class JpaConverter extends LowercaseEnumConverter<ModerationActionType> {
        public JpaConverter() {
            super(ModerationActionType.class);
        }
    }
}
