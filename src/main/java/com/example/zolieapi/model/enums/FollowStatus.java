package com.example.zolieapi.model.enums;

import com.example.zolieapi.common.LowercaseEnumConverter;
import jakarta.persistence.Converter;

public enum FollowStatus {
    PENDING,
    ACCEPTED;

    @Converter(autoApply = true)
    public static class JpaConverter extends LowercaseEnumConverter<FollowStatus> {
        public JpaConverter() {
            super(FollowStatus.class);
        }
    }
}
