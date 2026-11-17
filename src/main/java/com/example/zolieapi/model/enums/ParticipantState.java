package com.example.zolieapi.model.enums;

import com.example.zolieapi.common.LowercaseEnumConverter;
import jakarta.persistence.Converter;

public enum ParticipantState {
    INBOX,
    REQUEST;

    @Converter(autoApply = true)
    public static class JpaConverter extends LowercaseEnumConverter<ParticipantState> {
        public JpaConverter() {
            super(ParticipantState.class);
        }
    }
}
