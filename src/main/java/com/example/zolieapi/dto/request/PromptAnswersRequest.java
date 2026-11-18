package com.example.zolieapi.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record PromptAnswersRequest(
        @NotNull @Size(max = 3) @Schema(description = "Up to 3 answers, in display order.") List<@NotNull @Valid PromptAnswerRequest> answers
) {
}
