package com.example.zolieapi.common;

import jakarta.persistence.AttributeConverter;

public abstract class LowercaseEnumConverter<E extends Enum<E>> implements AttributeConverter<E, String> {

    private final Class<E> type;

    protected LowercaseEnumConverter(Class<E> type) {
        this.type = type;
    }

    @Override
    public String convertToDatabaseColumn(E value) {
        return Enums.value(value);
    }

    @Override
    public E convertToEntityAttribute(String value) {
        return value == null ? null : Enum.valueOf(type, value.toUpperCase());
    }
}
