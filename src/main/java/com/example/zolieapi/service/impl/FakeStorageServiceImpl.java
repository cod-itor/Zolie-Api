package com.example.zolieapi.service.impl;

import com.example.zolieapi.service.StorageService;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FakeStorageServiceImpl implements StorageService {

    private static final String BASE_URL = "https://fake-storage.zolie.local/";
    private static final Duration UPLOAD_TTL = Duration.ofMinutes(10);

    private final Map<String, StoredObject> objects = new ConcurrentHashMap<>();

    public FakeStorageServiceImpl() {
        log.warn("No storage endpoint configured: using fake storage. Uploads are assumed to succeed.");
    }

    @Override
    public PresignedUpload presignUpload(String objectKey, String mimeType, long sizeBytes) {
        objects.put(objectKey, new StoredObject(sizeBytes, mimeType));
        return new PresignedUpload(BASE_URL + objectKey + "?upload=1", Instant.now().plus(UPLOAD_TTL));
    }

    @Override
    public String downloadUrl(String objectKey) {
        return BASE_URL + objectKey;
    }

    @Override
    public Optional<StoredObject> stat(String objectKey) {
        return Optional.ofNullable(objects.get(objectKey));
    }

    @Override
    public void delete(String objectKey) {
        objects.remove(objectKey);
    }

    @Override
    public void download(String objectKey, Path target) {
        throw new UnsupportedOperationException("Fake storage holds no files");
    }

    @Override
    public void upload(Path source, String objectKey, String mimeType) {
        objects.put(objectKey, new StoredObject(source.toFile().length(), mimeType));
    }
}
