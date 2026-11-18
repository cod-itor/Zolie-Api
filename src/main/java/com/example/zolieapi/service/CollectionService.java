package com.example.zolieapi.service;

import com.example.zolieapi.common.CursorPage;
import com.example.zolieapi.dto.request.CollectionItemRequest;
import com.example.zolieapi.dto.response.CollectionResponse;
import com.example.zolieapi.dto.response.SavedItemResponse;
import java.util.List;
import java.util.UUID;

public interface CollectionService {

    List<CollectionResponse> list(UUID userId);

    CollectionResponse create(UUID userId, String name);

    CollectionResponse rename(UUID userId, UUID collectionId, String name);

    void delete(UUID userId, UUID collectionId);

    void addItem(UUID userId, UUID collectionId, CollectionItemRequest request);

    void removeItem(UUID userId, UUID collectionId, UUID postId, UUID reelId);

    CursorPage<SavedItemResponse> listItems(UUID userId, UUID collectionId, String cursor, Integer limit);
}
