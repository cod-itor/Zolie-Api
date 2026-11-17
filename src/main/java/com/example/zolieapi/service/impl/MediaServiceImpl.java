package com.example.zolieapi.service.impl;

import com.example.zolieapi.common.Enums;
import com.example.zolieapi.dto.request.UploadUrlRequest;
import com.example.zolieapi.dto.response.MediaResponse;
import com.example.zolieapi.dto.response.UploadUrlResponse;
import com.example.zolieapi.exception.ApiException;
import com.example.zolieapi.exception.ErrorCode;
import com.example.zolieapi.model.MediaAsset;
import com.example.zolieapi.model.enums.MediaKind;
import com.example.zolieapi.model.enums.MediaPurpose;
import com.example.zolieapi.model.enums.MediaStatus;
import com.example.zolieapi.repository.MediaAssetRepository;
import com.example.zolieapi.security.RateLimiter;
import com.example.zolieapi.service.MediaService;
import com.example.zolieapi.service.StorageService;
import com.example.zolieapi.service.StorageService.PresignedUpload;
import com.example.zolieapi.service.StorageService.StoredObject;
import java.time.Duration;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MediaServiceImpl implements MediaService {

    private static final long MAX_IMAGE_BYTES = 10L * 1024 * 1024;
    private static final long MAX_AUDIO_BYTES = 20L * 1024 * 1024;
    private static final long MAX_VIDEO_BYTES = 100L * 1024 * 1024;
    private static final int MAX_REEL_MS = 90_000;

    private static final Map<MediaKind, Map<String, String>> EXTENSIONS = Map.of(
            MediaKind.IMAGE, Map.of("image/jpeg", "jpg", "image/png", "png", "image/webp", "webp"),
            MediaKind.VIDEO, Map.of("video/mp4", "mp4", "video/webm", "webm"),
            MediaKind.AUDIO, Map.of("audio/mpeg", "mp3", "audio/mp4", "m4a", "audio/aac", "aac", "audio/ogg", "ogg"));

    private static final Map<MediaPurpose, Set<MediaKind>> KINDS_BY_PURPOSE = Map.of(
            MediaPurpose.PROFILE, Set.of(MediaKind.IMAGE),
            MediaPurpose.DATING, Set.of(MediaKind.IMAGE),
            MediaPurpose.CHAT, Set.of(MediaKind.IMAGE),
            MediaPurpose.REEL, Set.of(MediaKind.VIDEO),
            MediaPurpose.STORY, Set.of(MediaKind.IMAGE, MediaKind.VIDEO),
            MediaPurpose.POST, Set.of(MediaKind.IMAGE, MediaKind.VIDEO, MediaKind.AUDIO));

    private final MediaAssetRepository mediaAssetRepository;
    private final StorageService storageService;
    private final RateLimiter rateLimiter;

    @Override
    @Transactional
    public UploadUrlResponse createUpload(UUID userId, UploadUrlRequest request) {
        rateLimiter.check("upload-url", userId, 60, Duration.ofHours(1));
        MediaKind kind = Enums.require(MediaKind.class, request.kind(), "kind");
        MediaPurpose purpose = parsePurpose(request.purpose());
        String extension = validate(kind, purpose, request);

        String objectKey = "users/%s/%s/%s.%s".formatted(userId, Enums.value(purpose), UUID.randomUUID(), extension);
        MediaAsset asset = mediaAssetRepository.save(MediaAsset.builder()
                .ownerId(userId)
                .objectKey(objectKey)
                .kind(kind)
                .purpose(purpose)
                .mimeType(request.mimeType())
                .sizeBytes(request.sizeBytes())
                .width(request.width())
                .height(request.height())
                .durationMs(request.durationMs())
                .build());
        PresignedUpload upload = storageService.presignUpload(objectKey, request.mimeType(), request.sizeBytes());
        return new UploadUrlResponse(asset.getId(), upload.url(), upload.expiresAt());
    }

    @Override
    @Transactional(noRollbackFor = ApiException.class)
    public MediaResponse complete(UUID userId, UUID mediaId) {
        MediaAsset asset = requireOwned(userId, mediaId);
        if (asset.getStatus() != MediaStatus.UPLOADING) {
            return toResponse(asset);
        }
        Optional<StoredObject> stored = storageService.stat(asset.getObjectKey());
        if (stored.isEmpty()) {
            asset.setStatus(MediaStatus.FAILED);
            throw ApiException.rule("The file was not uploaded.");
        }
        if (!matches(asset, stored.get())) {
            storageService.delete(asset.getObjectKey());
            asset.setStatus(MediaStatus.FAILED);
            throw ApiException.rule("The uploaded file does not match the requested size or type.");
        }
        asset.setStatus(asset.getPurpose() == MediaPurpose.REEL ? MediaStatus.PROCESSING : MediaStatus.READY);
        return toResponse(asset);
    }

    @Override
    @Transactional(readOnly = true)
    public MediaResponse get(UUID userId, UUID mediaId) {
        return toResponse(requireOwned(userId, mediaId));
    }

    @Override
    @Transactional(readOnly = true)
    public MediaAsset requireUsable(UUID ownerId, UUID mediaId, MediaPurpose purpose) {
        MediaAsset asset = mediaAssetRepository.findById(mediaId)
                .filter(found -> found.getOwnerId().equals(ownerId))
                .orElseThrow(() -> ApiException.rule("Media " + mediaId + " was not found."));
        if (asset.getPurpose() != purpose) {
            throw ApiException.rule("Media " + mediaId + " was uploaded for a different purpose.");
        }
        boolean uploaded = asset.getStatus() == MediaStatus.READY
                || (purpose == MediaPurpose.REEL && asset.getStatus() == MediaStatus.PROCESSING);
        if (!uploaded) {
            throw ApiException.rule("Media " + mediaId + " is not uploaded yet. Call POST /media/{id}/complete first.");
        }
        return asset;
    }

    @Override
    @Transactional(readOnly = true)
    public String url(UUID mediaId) {
        return mediaId == null ? null : urls(Set.of(mediaId)).get(mediaId);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, String> urls(Collection<UUID> mediaIds) {
        Set<UUID> ids = mediaIds.stream().filter(Objects::nonNull).collect(Collectors.toSet());
        Map<UUID, String> urls = new HashMap<>();
        if (ids.isEmpty()) {
            return urls;
        }
        for (MediaAsset asset : mediaAssetRepository.findAllById(ids)) {
            if (asset.getStatus() == MediaStatus.READY) {
                urls.put(asset.getId(), storageService.downloadUrl(asset.getObjectKey()));
            }
        }
        return urls;
    }

    private MediaPurpose parsePurpose(String purpose) {
        if ("avatar".equalsIgnoreCase(purpose)) {
            return MediaPurpose.PROFILE;
        }
        MediaPurpose parsed = Enums.require(MediaPurpose.class, purpose, "purpose");
        if (parsed == MediaPurpose.REEL_THUMBNAIL) {
            throw ApiException.invalid("purpose is not allowed");
        }
        return parsed;
    }

    private String validate(MediaKind kind, MediaPurpose purpose, UploadUrlRequest request) {
        if (!KINDS_BY_PURPOSE.get(purpose).contains(kind)) {
            throw ApiException.rule("A " + Enums.value(kind) + " cannot be uploaded for " + Enums.value(purpose) + ".");
        }
        String extension = EXTENSIONS.get(kind).get(request.mimeType().toLowerCase());
        if (extension == null) {
            throw ApiException.rule("mimeType " + request.mimeType() + " is not supported.");
        }
        long maxBytes = switch (kind) {
            case IMAGE -> MAX_IMAGE_BYTES;
            case AUDIO -> MAX_AUDIO_BYTES;
            case VIDEO -> MAX_VIDEO_BYTES;
        };
        if (request.sizeBytes() > maxBytes) {
            throw new ApiException(ErrorCode.FILE_TOO_LARGE,
                    "The file is too big. The limit is " + maxBytes / (1024 * 1024) + " MB.");
        }
        if (kind != MediaKind.IMAGE && (request.durationMs() == null || request.durationMs() <= 0)) {
            throw ApiException.invalid("durationMs is required for video and audio");
        }
        if (purpose == MediaPurpose.REEL && request.durationMs() > MAX_REEL_MS) {
            throw ApiException.rule("Reels can be at most 90 seconds long.");
        }
        return extension;
    }

    private boolean matches(MediaAsset asset, StoredObject stored) {
        return stored.sizeBytes() == asset.getSizeBytes()
                && stored.contentType() != null
                && stored.contentType().toLowerCase().startsWith(asset.getMimeType().toLowerCase());
    }

    private MediaAsset requireOwned(UUID userId, UUID mediaId) {
        return mediaAssetRepository.findById(mediaId)
                .filter(asset -> asset.getOwnerId().equals(userId))
                .orElseThrow(ApiException::notFound);
    }

    private MediaResponse toResponse(MediaAsset asset) {
        String url = asset.getStatus() == MediaStatus.READY ? storageService.downloadUrl(asset.getObjectKey()) : null;
        return new MediaResponse(asset.getId(), Enums.value(asset.getKind()), Enums.value(asset.getPurpose()),
                Enums.value(asset.getStatus()), url);
    }
}
