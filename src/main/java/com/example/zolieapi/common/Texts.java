package com.example.zolieapi.common;

import com.example.zolieapi.exception.ApiException;
import com.example.zolieapi.exception.ErrorCode;
import java.text.Normalizer;
import lombok.experimental.UtilityClass;

@UtilityClass
public class Texts {

    public String clean(String text) {
        if (text == null) {
            return null;
        }
        String normalized = Normalizer.normalize(text.trim(), Normalizer.Form.NFC);
        return normalized.isEmpty() ? null : normalized;
    }

    public String clean(String text, int maxCodePoints, String field) {
        String cleaned = clean(text);
        if (cleaned != null && cleaned.codePointCount(0, cleaned.length()) > maxCodePoints) {
            throw new ApiException(ErrorCode.RULE_VIOLATION, field + " must be at most " + maxCodePoints + " characters");
        }
        return cleaned;
    }

    public String required(String text, int maxCodePoints, String field) {
        String cleaned = clean(text, maxCodePoints, field);
        if (cleaned == null) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, field + " is required");
        }
        return cleaned;
    }
}
