package com.example.zolieapi.model.enums;

import com.example.zolieapi.common.LowercaseEnumConverter;
import jakarta.persistence.Converter;

public enum HelpTopic {
    ACCOUNT,
    PRIVACY,
    DATING_SAFETY,
    REPORTING;

    @Converter(autoApply = true)
    public static class JpaConverter extends LowercaseEnumConverter<HelpTopic> {
        public JpaConverter() {
            super(HelpTopic.class);
        }
    }
}
