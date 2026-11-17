package com.example.zolieapi.model.enums;

import com.example.zolieapi.common.LowercaseEnumConverter;
import jakarta.persistence.Converter;

public enum AccountType {
    PUBLIC,
    PRIVATE;

    @Converter(autoApply = true)
    public static class JpaConverter extends LowercaseEnumConverter<AccountType> {
        public JpaConverter() {
            super(AccountType.class);
        }
    }
}
