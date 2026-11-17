package com.example.zolieapi.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record ReelViewsRequest(
        @NotEmpty @Valid List<ReelViewRequest> views
) {
}
