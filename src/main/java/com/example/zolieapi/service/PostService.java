package com.example.zolieapi.service;

import com.example.zolieapi.common.CursorPage;
import com.example.zolieapi.dto.request.CreatePostRequest;
import com.example.zolieapi.dto.request.UpdatePostRequest;
import com.example.zolieapi.dto.response.LikeResponse;
import com.example.zolieapi.dto.response.PollResponse;
import com.example.zolieapi.dto.response.PostResponse;
import com.example.zolieapi.dto.response.SaveResponse;
import com.example.zolieapi.dto.response.ShareResponse;
import com.example.zolieapi.dto.response.UserSummaryResponse;
import java.util.List;
import java.util.UUID;

public interface PostService {

    PostResponse create(UUID userId, UUID idempotencyKey, CreatePostRequest request);

    PostResponse get(UUID viewerId, UUID postId);

    PostResponse update(UUID userId, UUID postId, UpdatePostRequest request);

    void delete(UUID userId, UUID postId);

    CursorPage<PostResponse> feed(UUID viewerId, String cursor, Integer limit);

    CursorPage<PostResponse> listByAuthor(UUID viewerId, UUID authorId, String cursor, Integer limit);

    CursorPage<PostResponse> listSaved(UUID userId, String cursor, Integer limit);

    LikeResponse like(UUID userId, UUID postId, String reaction);

    LikeResponse unlike(UUID userId, UUID postId);

    CursorPage<UserSummaryResponse> listLikers(UUID viewerId, UUID postId, String cursor, Integer limit);

    SaveResponse save(UUID userId, UUID postId);

    SaveResponse unsave(UUID userId, UUID postId);

    ShareResponse share(UUID userId, UUID postId);

    void hide(UUID userId, UUID postId);

    void unhide(UUID userId, UUID postId);

    List<PostResponse> listPinned(UUID viewerId, UUID authorId);

    PollResponse vote(UUID userId, UUID postId, UUID optionId);
}
