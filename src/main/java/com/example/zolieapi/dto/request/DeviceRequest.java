package com.example.zolieapi.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DeviceRequest(
        @NotBlank @Size(max = 512) String token
) {
}
