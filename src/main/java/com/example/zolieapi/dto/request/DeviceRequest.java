package com.example.zolieapi.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DeviceRequest(
        @NotBlank @Size(max = 512) String token,
        @Size(max = 100) @Schema(example = "Samsung Galaxy A54") String deviceName
) {
}
