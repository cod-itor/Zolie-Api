package com.example.zolieapi.service;

import java.util.Optional;
import java.util.UUID;

public interface IdempotencyService {

    Optional<UUID> find(UUID userId, UUID key, String resourceType);

    void remember(UUID userId, UUID key, String resourceType, UUID resourceId);
}
