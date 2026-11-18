package com.example.zolieapi.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record StoryReplyRequest(
        @NotBlank @Size(max = 2000) String text
) {
}
