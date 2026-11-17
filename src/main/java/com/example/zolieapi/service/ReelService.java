package com.example.zolieapi.service;

import com.example.zolieapi.common.CursorPage;
import com.example.zolieapi.dto.request.CreateReelRequest;
import com.example.zolieapi.dto.request.ReelViewRequest;
import com.example.zolieapi.dto.response.LikeResponse;
import com.example.zolieapi.dto.response.ReelResponse;
import com.example.zolieapi.dto.response.SaveResponse;
import com.example.zolieapi.dto.response.ShareResponse;
import java.util.List;
import java.util.UUID;

public interface ReelService {

    ReelResponse create(UUID userId, UUID idempotencyKey, CreateReelRequest request);

    ReelResponse get(UUID viewerId, UUID reelId);

    void delete(UUID userId, UUID reelId);

    CursorPage<ReelResponse> feed(UUID viewerId, String tab, String cursor, Integer limit);

    CursorPage<ReelResponse> listByAuthor(UUID viewerId, UUID authorId, String cursor, Integer limit);

    CursorPage<ReelResponse> listSaved(UUID userId, String cursor, Integer limit);

    LikeResponse like(UUID userId, UUID reelId);

    LikeResponse unlike(UUID userId, UUID reelId);

    SaveResponse save(UUID userId, UUID reelId);

    SaveResponse unsave(UUID userId, UUID reelId);

    ShareResponse share(UUID userId, UUID reelId);

    void recordViews(UUID viewerId, List<ReelViewRequest> views);
}
