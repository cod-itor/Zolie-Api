package com.example.zolieapi.service;

import com.example.zolieapi.common.CursorPage;
import com.example.zolieapi.dto.request.CommentRequest;
import com.example.zolieapi.dto.response.CommentResponse;
import com.example.zolieapi.dto.response.LikeResponse;
import java.util.UUID;

public interface CommentService {

    CursorPage<CommentResponse> listForPost(UUID viewerId, UUID postId, String cursor, Integer limit);

    CursorPage<CommentResponse> listForReel(UUID viewerId, UUID reelId, String cursor, Integer limit);

    CursorPage<CommentResponse> listReplies(UUID viewerId, UUID commentId, String cursor, Integer limit);

    CommentResponse addToPost(UUID userId, UUID postId, CommentRequest request);

    CommentResponse addToReel(UUID userId, UUID reelId, CommentRequest request);

    void delete(UUID userId, UUID commentId);

    LikeResponse like(UUID userId, UUID commentId);

    LikeResponse unlike(UUID userId, UUID commentId);
}
