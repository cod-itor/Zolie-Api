package com.example.zolieapi.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PromptAnswerRequest(
        @NotNull Integer promptId,
        @NotBlank @Size(max = 300) String answer
) {
}
