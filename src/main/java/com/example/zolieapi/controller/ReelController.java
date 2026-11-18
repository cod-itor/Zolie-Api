package com.example.zolieapi.controller;

import com.example.zolieapi.common.CursorPage;
import com.example.zolieapi.dto.request.CreateReelRequest;
import com.example.zolieapi.dto.request.ReactionRequest;
import com.example.zolieapi.dto.request.ReelViewsRequest;
import com.example.zolieapi.dto.response.LikeResponse;
import com.example.zolieapi.dto.response.ReelResponse;
import com.example.zolieapi.dto.response.SaveResponse;
import com.example.zolieapi.dto.response.ShareResponse;
import com.example.zolieapi.security.CurrentUser;
import com.example.zolieapi.service.ReelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
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
@Tag(name = "12. Reels")
public class ReelController {

    private final ReelService reelService;
    private final CurrentUser currentUser;

    @GetMapping("/reels/feed")
    @Operation(summary = "Reels feed, newest first (guests can watch public Reels)")
    public CursorPage<ReelResponse> feed(
            @Parameter(description = "Use 'following' for Reels from accounts I follow")
            @RequestParam(required = false) String tab,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) Integer limit) {
        return reelService.feed(currentUser.idOrGuest(), tab, cursor, limit);
    }

    @PostMapping("/reels")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Publish a Reel from an uploaded video; it starts as processing")
    public ReelResponse create(@RequestHeader(value = "Idempotency-Key", required = false) UUID idempotencyKey,
                               @Valid @RequestBody CreateReelRequest request) {
        return reelService.create(currentUser.id(), idempotencyKey, request);
    }

    @PostMapping("/reels/views")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Count a batch of Reel views")
    public void recordViews(@Valid @RequestBody ReelViewsRequest request) {
        reelService.recordViews(currentUser.id(), request.views());
    }

    @GetMapping("/reels/{id}")
    @Operation(summary = "Get one Reel, including its processing status")
    public ReelResponse get(@PathVariable UUID id) {
        return reelService.get(currentUser.idOrGuest(), id);
    }

    @DeleteMapping("/reels/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete my Reel")
    public void delete(@PathVariable UUID id) {
        reelService.delete(currentUser.id(), id);
    }

    @GetMapping("/users/{id}/reels")
    @Operation(summary = "List an account's Reels (guests can see public accounts)")
    public CursorPage<ReelResponse> listByAuthor(@PathVariable UUID id,
                                                 @RequestParam(required = false) String cursor,
                                                 @RequestParam(required = false) Integer limit) {
        return reelService.listByAuthor(currentUser.idOrGuest(), id, cursor, limit);
    }

    @PutMapping("/reels/{id}/like")
    @Operation(summary = "Like a Reel")
    public LikeResponse like(@PathVariable UUID id, @RequestBody(required = false) ReactionRequest request) {
        return reelService.like(currentUser.id(), id, request == null ? null : request.reaction());
    }

    @DeleteMapping("/reels/{id}/like")
    @Operation(summary = "Remove my like from a Reel")
    public LikeResponse unlike(@PathVariable UUID id) {
        return reelService.unlike(currentUser.id(), id);
    }

    @PutMapping("/reels/{id}/save")
    @Operation(summary = "Save a Reel")
    public SaveResponse save(@PathVariable UUID id) {
        return reelService.save(currentUser.id(), id);
    }

    @DeleteMapping("/reels/{id}/save")
    @Operation(summary = "Remove a Reel from my saved items")
    public SaveResponse unsave(@PathVariable UUID id) {
        return reelService.unsave(currentUser.id(), id);
    }

    @PostMapping("/reels/{id}/shares")
    @Operation(summary = "Count a share outside the app")
    public ShareResponse share(@PathVariable UUID id) {
        return reelService.share(currentUser.id(), id);
    }
}
