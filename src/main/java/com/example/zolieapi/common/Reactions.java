package com.example.zolieapi.common;

import com.example.zolieapi.exception.ApiException;
import java.util.List;
import lombok.experimental.UtilityClass;

@UtilityClass
public class Reactions {

    public static final String DEFAULT = "💜";

    private static final List<String> ALLOWED = List.of("🙏", "💜", "😂", "🔥", "👏", "😍", "😮");

    public String require(String reaction) {
        if (reaction == null || reaction.isBlank()) {
            throw ApiException.invalid("reaction is required");
        }
        return parse(reaction);
    }

    public String parse(String reaction) {
        if (reaction == null || reaction.isBlank()) {
            return DEFAULT;
        }
        String trimmed = reaction.trim();
        if (!ALLOWED.contains(trimmed)) {
            throw ApiException.invalid("reaction must be one of: " + String.join(" ", ALLOWED));
        }
        return trimmed;
    }
}
