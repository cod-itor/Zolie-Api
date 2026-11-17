package com.example.zolieapi.model.enums;

import com.example.zolieapi.common.LowercaseEnumConverter;
import jakarta.persistence.Converter;

public enum StickerType {
    QUESTION,
    EMOJI_SLIDER,
    COUNTDOWN;

    @Converter(autoApply = true)
    public static class JpaConverter extends LowercaseEnumConverter<StickerType> {
        public JpaConverter() {
            super(StickerType.class);
        }
    }
}
