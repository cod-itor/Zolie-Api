package com.example.zolieapi.service;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;

public interface StorageService {

    PresignedUpload presignUpload(String objectKey, String mimeType, long sizeBytes);

    String downloadUrl(String objectKey);

    Optional<StoredObject> stat(String objectKey);

    void delete(String objectKey);

    void download(String objectKey, Path target);

    void upload(Path source, String objectKey, String mimeType);

    record PresignedUpload(String url, Instant expiresAt) {
    }

    record StoredObject(long sizeBytes, String contentType) {
    }
}
