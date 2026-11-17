package com.example.zolieapi.model.enums;

import com.example.zolieapi.common.LowercaseEnumConverter;
import jakarta.persistence.Converter;

public enum Gender {
    WOMAN,
    MAN,
    NONBINARY,
    OTHER;

    @Converter(autoApply = true)
    public static class JpaConverter extends LowercaseEnumConverter<Gender> {
        public JpaConverter() {
            super(Gender.class);
        }
    }
}
