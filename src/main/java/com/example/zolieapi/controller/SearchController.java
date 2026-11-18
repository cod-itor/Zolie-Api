package com.example.zolieapi.controller;

import com.example.zolieapi.common.CursorPage;
import com.example.zolieapi.security.CurrentUser;
import com.example.zolieapi.service.SearchService;
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
@Tag(name = "17. Search")
public class SearchController {

    private final SearchService searchService;
    private final CurrentUser currentUser;

    @GetMapping("/search")
    @Operation(summary = "Search people, public posts and Reels, hashtags or places (works for Khmer)")
    public CursorPage<?> search(
            @RequestParam String q,
            @Parameter(schema = @Schema(allowableValues = {"users", "posts", "reels", "hashtags", "places"}))
            @RequestParam(defaultValue = "users") String type,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) Integer limit) {
        return searchService.search(currentUser.id(), q, type, cursor, limit);
    }
}
