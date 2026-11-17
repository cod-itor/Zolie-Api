package com.example.zolieapi.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ResolveLoginRequest(
        @NotBlank String username
) {
}
