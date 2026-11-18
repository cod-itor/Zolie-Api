package com.example.zolieapi.controller;

import com.example.zolieapi.common.CursorPage;
import com.example.zolieapi.dto.request.CreatePostRequest;
import com.example.zolieapi.dto.request.PollVoteRequest;
import com.example.zolieapi.dto.request.ReactionRequest;
import com.example.zolieapi.dto.request.UpdatePostRequest;
import com.example.zolieapi.dto.response.LikeResponse;
import com.example.zolieapi.dto.response.PollResponse;
import com.example.zolieapi.dto.response.PostResponse;
import com.example.zolieapi.dto.response.SaveResponse;
import com.example.zolieapi.dto.response.ShareResponse;
import com.example.zolieapi.dto.response.UserSummaryResponse;
import com.example.zolieapi.security.CurrentUser;
import com.example.zolieapi.service.PostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "10. Posts")
public class PostController {

    private final PostService postService;
    private final CurrentUser currentUser;

    @PostMapping("/posts")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a text, photo, carousel, video, poll or audio post")
    public PostResponse create(@RequestHeader(value = "Idempotency-Key", required = false) UUID idempotencyKey,
                               @Valid @RequestBody CreatePostRequest request) {
        return postService.create(currentUser.id(), idempotencyKey, request);
    }

    @GetMapping("/posts/{id}")
    @Operation(summary = "Get one post (guests can see public posts)")
    public PostResponse get(@PathVariable UUID id) {
        return postService.get(currentUser.idOrGuest(), id);
    }

    @PatchMapping("/posts/{id}")
    @Operation(summary = "Edit my post: caption, tags, audience, location, pin, hide likes, comments on or off")
    public PostResponse update(@PathVariable UUID id, @Valid @RequestBody UpdatePostRequest request) {
        return postService.update(currentUser.id(), id, request);
    }

    @DeleteMapping("/posts/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete my post")
    public void delete(@PathVariable UUID id) {
        postService.delete(currentUser.id(), id);
    }

    @GetMapping("/users/{id}/posts")
    @Operation(summary = "List an account's posts (guests can see public accounts)")
    public CursorPage<PostResponse> listByAuthor(@PathVariable UUID id,
                                                 @RequestParam(required = false) String cursor,
                                                 @RequestParam(required = false) Integer limit) {
        return postService.listByAuthor(currentUser.idOrGuest(), id, cursor, limit);
    }

    @PutMapping("/posts/{id}/like")
    @Operation(summary = "Like a post or react with an emoji; calling it again changes the reaction")
    public LikeResponse like(@PathVariable UUID id, @RequestBody(required = false) ReactionRequest request) {
        return postService.like(currentUser.id(), id, request == null ? null : request.reaction());
    }

    @DeleteMapping("/posts/{id}/like")
    @Operation(summary = "Remove my like from a post")
    public LikeResponse unlike(@PathVariable UUID id) {
        return postService.unlike(currentUser.id(), id);
    }

    @GetMapping("/posts/{id}/likes")
    @Operation(summary = "List who liked a post")
    public CursorPage<UserSummaryResponse> listLikers(@PathVariable UUID id,
                                                      @RequestParam(required = false) String cursor,
                                                      @RequestParam(required = false) Integer limit) {
        return postService.listLikers(currentUser.id(), id, cursor, limit);
    }

    @PutMapping("/posts/{id}/save")
    @Operation(summary = "Save a post")
    public SaveResponse save(@PathVariable UUID id) {
        return postService.save(currentUser.id(), id);
    }

    @DeleteMapping("/posts/{id}/save")
    @Operation(summary = "Remove a post from my saved items")
    public SaveResponse unsave(@PathVariable UUID id) {
        return postService.unsave(currentUser.id(), id);
    }

    @PutMapping("/posts/{id}/hide")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Hide a post from my feed (Not interested)")
    public void hide(@PathVariable UUID id) {
        postService.hide(currentUser.id(), id);
    }

    @DeleteMapping("/posts/{id}/hide")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Show a hidden post in my feed again")
    public void unhide(@PathVariable UUID id) {
        postService.unhide(currentUser.id(), id);
    }

    @GetMapping("/users/{id}/posts/pinned")
    @Operation(summary = "List the posts an account pinned to its profile")
    public List<PostResponse> listPinned(@PathVariable UUID id) {
        return postService.listPinned(currentUser.idOrGuest(), id);
    }

    @PostMapping("/posts/{id}/shares")
    @Operation(summary = "Count a share outside the app")
    public ShareResponse share(@PathVariable UUID id) {
        return postService.share(currentUser.id(), id);
    }

    @PostMapping("/posts/{id}/poll/votes")
    @Operation(summary = "Vote in a poll (one vote per person)")
    public PollResponse vote(@PathVariable UUID id, @Valid @RequestBody PollVoteRequest request) {
        return postService.vote(currentUser.id(), id, request.optionId());
    }
}
