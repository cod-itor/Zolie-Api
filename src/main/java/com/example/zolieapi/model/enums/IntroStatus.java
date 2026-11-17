package com.example.zolieapi.model.enums;

import com.example.zolieapi.common.LowercaseEnumConverter;
import jakarta.persistence.Converter;

public enum IntroStatus {
    PENDING,
    MATCHED,
    PASSED;

    @Converter(autoApply = true)
    public static class JpaConverter extends LowercaseEnumConverter<IntroStatus> {
        public JpaConverter() {
            super(IntroStatus.class);
        }
    }
}
