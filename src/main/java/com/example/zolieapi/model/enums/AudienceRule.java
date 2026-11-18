package com.example.zolieapi.model.enums;

import com.example.zolieapi.common.LowercaseEnumConverter;
import jakarta.persistence.Converter;

public enum AudienceRule {
    EVERYONE,
    FOLLOWERS,
    NOBODY;

    @Converter(autoApply = true)
    public static class JpaConverter extends LowercaseEnumConverter<AudienceRule> {
        public JpaConverter() {
            super(AudienceRule.class);
        }
    }
}
