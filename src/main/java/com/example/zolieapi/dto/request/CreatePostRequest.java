package com.example.zolieapi.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.UUID;

public record CreatePostRequest(
        @NotBlank @Schema(allowableValues = {"text", "photo", "carousel", "video", "poll", "audio"}) String type,
        @Schema(example = "Sunset in Kampot") String caption,
        @Schema(description = "Uploaded media IDs in display order. Not used for text and poll posts.") List<UUID> mediaIds,
        @Valid PollRequest poll,
        List<UUID> taggedUserIds,
        @Schema(allowableValues = {"public", "followers"}) String visibility
) {
}
