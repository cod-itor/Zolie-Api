package com.example.zolieapi.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record UpdateInterestsRequest(
        @NotNull @Schema(example = "[1, 2, 3]") List<Integer> interestIds
) {
}
