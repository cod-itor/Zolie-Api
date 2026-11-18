package com.example.zolieapi.model.enums;

import com.example.zolieapi.common.LowercaseEnumConverter;
import jakarta.persistence.Converter;

public enum PlaceType {
    PROVINCE,
    CITY,
    DISTRICT,
    LANDMARK,
    VENUE;

    @Converter(autoApply = true)
    public static class JpaConverter extends LowercaseEnumConverter<PlaceType> {
        public JpaConverter() {
            super(PlaceType.class);
        }
    }
}
