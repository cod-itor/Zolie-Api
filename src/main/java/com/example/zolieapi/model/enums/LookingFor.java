package com.example.zolieapi.model.enums;

import com.example.zolieapi.common.LowercaseEnumConverter;
import jakarta.persistence.Converter;

public enum LookingFor {
    RELATIONSHIP,
    CASUAL,
    FRIENDS,
    NOT_SURE;

    @Converter(autoApply = true)
    public static class JpaConverter extends LowercaseEnumConverter<LookingFor> {
        public JpaConverter() {
            super(LookingFor.class);
        }
    }
}
