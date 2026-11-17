package com.example.zolieapi.model.enums;

import com.example.zolieapi.common.LowercaseEnumConverter;
import jakarta.persistence.Converter;

public enum UserRole {
    USER,
    ADMIN;

    @Converter(autoApply = true)
    public static class JpaConverter extends LowercaseEnumConverter<UserRole> {
        public JpaConverter() {
            super(UserRole.class);
        }
    }
}
