package com.example.zolieapi.model.enums;

import com.example.zolieapi.common.LowercaseEnumConverter;
import jakarta.persistence.Converter;

public enum AccountStatus {
    ACTIVE,
    SUSPENDED,
    DELETED;

    @Converter(autoApply = true)
    public static class JpaConverter extends LowercaseEnumConverter<AccountStatus> {
        public JpaConverter() {
            super(AccountStatus.class);
        }
    }
}
