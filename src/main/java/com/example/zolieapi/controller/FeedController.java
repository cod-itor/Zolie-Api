package com.example.zolieapi.controller;

import com.example.zolieapi.common.CursorPage;
import com.example.zolieapi.dto.response.PostResponse;
import com.example.zolieapi.security.CurrentUser;
import com.example.zolieapi.security.Viewer;
import com.example.zolieapi.service.PostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/feed")
@RequiredArgsConstructor
@Tag(name = "09. Feed")
public class FeedController {

    private final PostService postService;
    private final CurrentUser currentUser;

    @GetMapping("/home")
    @Operation(summary = "Home feed: posts I am allowed to see, newest first")
    public CursorPage<PostResponse> home(@RequestParam(required = false) String cursor,
                                         @RequestParam(required = false) Integer limit) {
        return postService.feed(currentUser.id(), cursor, limit);
    }

    @GetMapping("/public")
    @SecurityRequirements
    @Operation(summary = "Public posts for guests (no token needed)")
    public CursorPage<PostResponse> publicFeed(@RequestParam(required = false) String cursor,
                                               @RequestParam(required = false) Integer limit) {
        return postService.feed(Viewer.GUEST, cursor, limit);
    }
}
