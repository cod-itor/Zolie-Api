package com.example.zolieapi.controller;

import com.example.zolieapi.common.CursorPage;
import com.example.zolieapi.dto.request.CreateStoryRequest;
import com.example.zolieapi.dto.request.StickerAnswerRequest;
import com.example.zolieapi.dto.request.StoryReplyRequest;
import com.example.zolieapi.dto.response.MessageResponse;
import com.example.zolieapi.dto.response.StickerAnswersResponse;
import com.example.zolieapi.dto.response.StoryResponse;
import com.example.zolieapi.dto.response.StoryRingResponse;
import com.example.zolieapi.dto.response.UserSummaryResponse;
import com.example.zolieapi.security.CurrentUser;
import com.example.zolieapi.service.StoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
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
@Tag(name = "13. Stories")
public class StoryController {

    private final StoryService storyService;
    private final CurrentUser currentUser;

    @GetMapping("/stories/feed")
    @Operation(summary = "Story rings: accounts with active stories I may see")
    public List<StoryRingResponse> feed() {
        return storyService.feed(currentUser.id());
    }

    @GetMapping("/users/{id}/stories")
    @Operation(summary = "List one account's active stories, oldest first")
    public List<StoryResponse> listByAuthor(@PathVariable UUID id) {
        return storyService.listByAuthor(currentUser.id(), id);
    }

    @PostMapping("/stories")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Post a 24-hour story for everyone or Close Friends, with optional stickers")
    public StoryResponse create(@RequestHeader(value = "Idempotency-Key", required = false) UUID idempotencyKey,
                                @Valid @RequestBody CreateStoryRequest request) {
        return storyService.create(currentUser.id(), idempotencyKey, request);
    }

    @DeleteMapping("/stories/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete my story early")
    public void delete(@PathVariable UUID id) {
        storyService.delete(currentUser.id(), id);
    }

    @PostMapping("/stories/{id}/views")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Mark a story as seen")
    public void markSeen(@PathVariable UUID id) {
        storyService.markSeen(currentUser.id(), id);
    }

    @GetMapping("/stories/{id}/viewers")
    @Operation(summary = "List who saw my story")
    public CursorPage<UserSummaryResponse> listViewers(@PathVariable UUID id,
                                                       @RequestParam(required = false) String cursor,
                                                       @RequestParam(required = false) Integer limit) {
        return storyService.listViewers(currentUser.id(), id, cursor, limit);
    }

    @PostMapping("/stories/{id}/replies")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Reply to a story; it arrives as a private message")
    public MessageResponse reply(@PathVariable UUID id, @Valid @RequestBody StoryReplyRequest request) {
        return storyService.reply(currentUser.id(), id, request.text());
    }

    @PostMapping("/stories/{id}/stickers/{stickerId}/responses")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Answer a question or emoji slider sticker (one answer per person)")
    public MessageResponse answerSticker(@PathVariable UUID id, @PathVariable UUID stickerId,
                                         @Valid @RequestBody StickerAnswerRequest request) {
        return storyService.answerSticker(currentUser.id(), id, stickerId, request);
    }

    @GetMapping("/stories/{id}/stickers/{stickerId}/responses")
    @Operation(summary = "List the answers to my sticker, with the slider average")
    public StickerAnswersResponse listStickerAnswers(@PathVariable UUID id, @PathVariable UUID stickerId,
                                                     @RequestParam(required = false) String cursor,
                                                     @RequestParam(required = false) Integer limit) {
        return storyService.listStickerAnswers(currentUser.id(), id, stickerId, cursor, limit);
    }

    @PutMapping("/stories/{id}/stickers/{stickerId}/reminder")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remind me when a countdown sticker ends")
    public void setReminder(@PathVariable UUID id, @PathVariable UUID stickerId) {
        storyService.setReminder(currentUser.id(), id, stickerId, true);
    }

    @DeleteMapping("/stories/{id}/stickers/{stickerId}/reminder")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove my countdown reminder")
    public void removeReminder(@PathVariable UUID id, @PathVariable UUID stickerId) {
        storyService.setReminder(currentUser.id(), id, stickerId, false);
    }
}
