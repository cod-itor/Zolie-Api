package com.example.zolieapi.model.enums;

import com.example.zolieapi.common.LowercaseEnumConverter;
import jakarta.persistence.Converter;

public enum DatingLikeType {
    SWIPE,
    INTRO;

    @Converter(autoApply = true)
    public static class JpaConverter extends LowercaseEnumConverter<DatingLikeType> {
        public JpaConverter() {
            super(DatingLikeType.class);
        }
    }
}
