package com.example.zolieapi.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public record CommentRequest(
        @NotBlank @Schema(example = "Beautiful! @sophea look at this") String text,
        @Schema(description = "Set to reply to a comment.") UUID parentId
) {
}
