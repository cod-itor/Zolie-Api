package com.example.zolieapi.model.enums;

import com.example.zolieapi.common.LowercaseEnumConverter;
import jakarta.persistence.Converter;

public enum MatchStatus {
    ACTIVE,
    UNMATCHED,
    BLOCKED;

    @Converter(autoApply = true)
    public static class JpaConverter extends LowercaseEnumConverter<MatchStatus> {
        public JpaConverter() {
            super(MatchStatus.class);
        }
    }
}
