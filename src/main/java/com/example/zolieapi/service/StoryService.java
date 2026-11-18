package com.example.zolieapi.service;

import com.example.zolieapi.common.CursorPage;
import com.example.zolieapi.dto.request.CreateStoryRequest;
import com.example.zolieapi.dto.request.StickerAnswerRequest;
import com.example.zolieapi.dto.response.MessageResponse;
import com.example.zolieapi.dto.response.StickerAnswersResponse;
import com.example.zolieapi.dto.response.StoryResponse;
import com.example.zolieapi.dto.response.StoryRingResponse;
import com.example.zolieapi.dto.response.UserSummaryResponse;
import java.util.List;
import java.util.UUID;

public interface StoryService {

    StoryResponse create(UUID userId, UUID idempotencyKey, CreateStoryRequest request);

    void delete(UUID userId, UUID storyId);

    List<StoryRingResponse> feed(UUID userId);

    List<StoryResponse> listByAuthor(UUID viewerId, UUID authorId);

    void markSeen(UUID viewerId, UUID storyId);

    CursorPage<UserSummaryResponse> listViewers(UUID userId, UUID storyId, String cursor, Integer limit);

    MessageResponse reply(UUID userId, UUID storyId, String text);

    MessageResponse answerSticker(UUID userId, UUID storyId, UUID stickerId, StickerAnswerRequest request);

    StickerAnswersResponse listStickerAnswers(UUID userId, UUID storyId, UUID stickerId, String cursor, Integer limit);

    void setReminder(UUID userId, UUID storyId, UUID stickerId, boolean remind);
}
