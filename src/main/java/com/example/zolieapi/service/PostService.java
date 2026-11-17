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
import java.util.UUID;

public interface PostService {

    PostResponse create(UUID userId, UUID idempotencyKey, CreatePostRequest request);

    PostResponse get(UUID viewerId, UUID postId);

    PostResponse update(UUID userId, UUID postId, UpdatePostRequest request);

    void delete(UUID userId, UUID postId);

    CursorPage<PostResponse> feed(UUID viewerId, String cursor, Integer limit);

    CursorPage<PostResponse> listByAuthor(UUID viewerId, UUID authorId, String cursor, Integer limit);

    CursorPage<PostResponse> listSaved(UUID userId, String cursor, Integer limit);

    LikeResponse like(UUID userId, UUID postId);

    LikeResponse unlike(UUID userId, UUID postId);

    CursorPage<UserSummaryResponse> listLikers(UUID viewerId, UUID postId, String cursor, Integer limit);

    SaveResponse save(UUID userId, UUID postId);

    SaveResponse unsave(UUID userId, UUID postId);

    ShareResponse share(UUID userId, UUID postId);

    PollResponse vote(UUID userId, UUID postId, UUID optionId);
}
