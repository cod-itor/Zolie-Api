package com.example.zolieapi.common;

import com.example.zolieapi.exception.ApiException;
import com.example.zolieapi.exception.ErrorCode;
import java.util.Arrays;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;

@UtilityClass
public class Enums {

    public String value(Enum<?> value) {
        return value == null ? null : value.name().toLowerCase();
    }

    public <E extends Enum<E>> E parse(Class<E> type, String value, String field) {
        if (value == null) {
            return null;
        }
        try {
            return Enum.valueOf(type, value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            String allowed = Arrays.stream(type.getEnumConstants()).map(Enums::value).collect(Collectors.joining(", "));
            throw new ApiException(ErrorCode.VALIDATION_FAILED, field + " must be one of: " + allowed);
        }
    }

    public <E extends Enum<E>> E require(Class<E> type, String value, String field) {
        if (value == null || value.isBlank()) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, field + " is required");
        }
        return parse(type, value, field);
    }
}
