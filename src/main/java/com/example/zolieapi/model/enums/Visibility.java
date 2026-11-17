package com.example.zolieapi.model.enums;

import com.example.zolieapi.common.LowercaseEnumConverter;
import jakarta.persistence.Converter;

public enum Visibility {
    PUBLIC,
    FOLLOWERS;

    @Converter(autoApply = true)
    public static class JpaConverter extends LowercaseEnumConverter<Visibility> {
        public JpaConverter() {
            super(Visibility.class);
        }
    }
}
