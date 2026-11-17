package com.example.zolieapi.model.enums;

import com.example.zolieapi.common.LowercaseEnumConverter;
import jakarta.persistence.Converter;

public enum MediaStatus {
    UPLOADING,
    PROCESSING,
    READY,
    FAILED;

    @Converter(autoApply = true)
    public static class JpaConverter extends LowercaseEnumConverter<MediaStatus> {
        public JpaConverter() {
            super(MediaStatus.class);
        }
    }
}
