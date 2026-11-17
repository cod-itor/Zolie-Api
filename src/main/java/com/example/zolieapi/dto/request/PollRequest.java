package com.example.zolieapi.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import java.time.Instant;
import java.util.List;

public record PollRequest(
        @NotEmpty @Schema(example = "[\"Amok\", \"Lok lak\"]") List<String> options,
        Instant endsAt
) {
}
