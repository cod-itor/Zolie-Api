package com.example.zolieapi.service;

import com.example.zolieapi.dto.request.UploadUrlRequest;
import com.example.zolieapi.dto.response.MediaResponse;
import com.example.zolieapi.dto.response.UploadUrlResponse;
import com.example.zolieapi.model.MediaAsset;
import com.example.zolieapi.model.enums.MediaPurpose;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;

public interface MediaService {

    UploadUrlResponse createUpload(UUID userId, UploadUrlRequest request);

    MediaResponse complete(UUID userId, UUID mediaId);

    MediaResponse get(UUID userId, UUID mediaId);

    MediaAsset requireUsable(UUID ownerId, UUID mediaId, MediaPurpose purpose);

    String url(UUID mediaId);

    Map<UUID, String> urls(Collection<UUID> mediaIds);
}
