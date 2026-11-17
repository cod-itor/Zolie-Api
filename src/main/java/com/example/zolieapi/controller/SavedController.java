package com.example.zolieapi.controller;

import com.example.zolieapi.common.CursorPage;
import com.example.zolieapi.exception.ApiException;
import com.example.zolieapi.security.CurrentUser;
import com.example.zolieapi.service.PostService;
import com.example.zolieapi.service.ReelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "10. Posts")
public class SavedController {

    private final PostService postService;
    private final ReelService reelService;
    private final CurrentUser currentUser;

    @GetMapping("/me/saved")
    @Operation(summary = "List my saved posts or saved Reels")
    public CursorPage<?> listSaved(
            @Parameter(schema = @Schema(allowableValues = {"post", "reel"}))
            @RequestParam(defaultValue = "post") String type,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) Integer limit) {
        return switch (type.toLowerCase()) {
            case "post" -> postService.listSaved(currentUser.id(), cursor, limit);
            case "reel" -> reelService.listSaved(currentUser.id(), cursor, limit);
            default -> throw ApiException.invalid("type must be one of: post, reel");
        };
    }
}
