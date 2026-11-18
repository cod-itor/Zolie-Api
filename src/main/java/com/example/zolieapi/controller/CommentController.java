package com.example.zolieapi.controller;

import com.example.zolieapi.common.CursorPage;
import com.example.zolieapi.dto.request.CommentRequest;
import com.example.zolieapi.dto.response.CommentResponse;
import com.example.zolieapi.dto.response.LikeResponse;
import com.example.zolieapi.security.CurrentUser;
import com.example.zolieapi.service.CommentService;
import io.swagger.v3.oas.annotations.Operation;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "11. Comments")
public class CommentController {

    private final CommentService commentService;
    private final CurrentUser currentUser;

    @GetMapping("/posts/{id}/comments")
    @Operation(summary = "List the top-level comments of a post, newest first")
    public CursorPage<CommentResponse> listForPost(@PathVariable UUID id,
                                                   @RequestParam(required = false) String cursor,
                                                   @RequestParam(required = false) Integer limit) {
        return commentService.listForPost(currentUser.idOrGuest(), id, cursor, limit);
    }

    @PostMapping("/posts/{id}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Comment on a post or reply to a comment; @usernames are notified")
    public CommentResponse addToPost(@PathVariable UUID id, @Valid @RequestBody CommentRequest request) {
        return commentService.addToPost(currentUser.id(), id, request);
    }

    @GetMapping("/reels/{id}/comments")
    @Operation(summary = "List the top-level comments of a Reel, newest first")
    public CursorPage<CommentResponse> listForReel(@PathVariable UUID id,
                                                   @RequestParam(required = false) String cursor,
                                                   @RequestParam(required = false) Integer limit) {
        return commentService.listForReel(currentUser.idOrGuest(), id, cursor, limit);
    }

    @PostMapping("/reels/{id}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Comment on a Reel or reply to a comment")
    public CommentResponse addToReel(@PathVariable UUID id, @Valid @RequestBody CommentRequest request) {
        return commentService.addToReel(currentUser.id(), id, request);
    }

    @GetMapping("/comments/{id}/replies")
    @Operation(summary = "List the replies to a comment, oldest first")
    public CursorPage<CommentResponse> listReplies(@PathVariable UUID id,
                                                   @RequestParam(required = false) String cursor,
                                                   @RequestParam(required = false) Integer limit) {
        return commentService.listReplies(currentUser.idOrGuest(), id, cursor, limit);
    }

    @PutMapping("/comments/{id}/like")
    @Operation(summary = "Like a comment")
    public LikeResponse like(@PathVariable UUID id) {
        return commentService.like(currentUser.id(), id);
    }

    @DeleteMapping("/comments/{id}/like")
    @Operation(summary = "Remove my like from a comment")
    public LikeResponse unlike(@PathVariable UUID id) {
        return commentService.unlike(currentUser.id(), id);
    }

    @DeleteMapping("/comments/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a comment (its author or the content author)")
    public void delete(@PathVariable UUID id) {
        commentService.delete(currentUser.id(), id);
    }
}
