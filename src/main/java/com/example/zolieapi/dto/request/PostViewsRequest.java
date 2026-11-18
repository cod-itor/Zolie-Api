package com.example.zolieapi.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record PostViewsRequest(
        @NotEmpty @Size(max = 200) List<UUID> postIds
) {
}
