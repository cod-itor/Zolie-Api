package com.example.zolieapi.controller;

import com.example.zolieapi.common.CursorPage;
import com.example.zolieapi.dto.response.HashtagResponse;
import com.example.zolieapi.dto.response.PostResponse;
import com.example.zolieapi.dto.response.ReelResponse;
import com.example.zolieapi.security.CurrentUser;
import com.example.zolieapi.service.HashtagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/hashtags")
@RequiredArgsConstructor
@Tag(name = "24. Hashtags")
public class HashtagController {

    private final HashtagService hashtagService;
    private final CurrentUser currentUser;

    @GetMapping("/trending")
    @Operation(summary = "Trending hashtags by public posts in the last 7 days")
    public List<HashtagResponse> trending() {
        return hashtagService.trending();
    }

    @GetMapping
    @Operation(summary = "Search hashtags (Khmer and Latin)")
    public List<HashtagResponse> search(@RequestParam(required = false) String q) {
        return hashtagService.search(q);
    }

    @GetMapping("/{tag}/posts")
    @Operation(summary = "List public posts with a hashtag (send the tag without #)")
    public CursorPage<PostResponse> listPosts(@PathVariable String tag,
                                              @RequestParam(required = false) String cursor,
                                              @RequestParam(required = false) Integer limit) {
        return hashtagService.listPosts(currentUser.idOrGuest(), tag, cursor, limit);
    }

    @GetMapping("/{tag}/reels")
    @Operation(summary = "List public Reels with a hashtag")
    public CursorPage<ReelResponse> listReels(@PathVariable String tag,
                                              @RequestParam(required = false) String cursor,
                                              @RequestParam(required = false) Integer limit) {
        return hashtagService.listReels(currentUser.idOrGuest(), tag, cursor, limit);
    }
}
