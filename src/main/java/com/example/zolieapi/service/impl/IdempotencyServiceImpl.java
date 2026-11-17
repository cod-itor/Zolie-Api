package com.example.zolieapi.service.impl;

import com.example.zolieapi.exception.ApiException;
import com.example.zolieapi.model.IdempotencyKey;
import com.example.zolieapi.model.id.IdempotencyKeyId;
import com.example.zolieapi.repository.IdempotencyKeyRepository;
import com.example.zolieapi.service.IdempotencyService;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class IdempotencyServiceImpl implements IdempotencyService {

    private final IdempotencyKeyRepository idempotencyKeyRepository;

    @Override
    @Transactional(readOnly = true)
    public Optional<UUID> find(UUID userId, UUID key, String resourceType) {
        if (key == null) {
            return Optional.empty();
        }
        Optional<IdempotencyKey> existing = idempotencyKeyRepository.findById(new IdempotencyKeyId(userId, key));
        if (existing.isPresent() && !existing.get().getResourceType().equals(resourceType)) {
            throw ApiException.rule("This Idempotency-Key was already used for a different request.");
        }
        return existing.map(IdempotencyKey::getResourceId);
    }

    @Override
    @Transactional
    public void remember(UUID userId, UUID key, String resourceType, UUID resourceId) {
        if (key == null) {
            return;
        }
        idempotencyKeyRepository.save(IdempotencyKey.builder()
                .id(new IdempotencyKeyId(userId, key))
                .resourceType(resourceType)
                .resourceId(resourceId)
                .build());
    }
}
