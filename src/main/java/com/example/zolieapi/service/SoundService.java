package com.example.zolieapi.service;

import com.example.zolieapi.dto.request.CreateSoundRequest;
import com.example.zolieapi.dto.response.SoundResponse;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface SoundService {

    SoundResponse create(UUID userId, CreateSoundRequest request);

    List<SoundResponse> search(String query);

    SoundResponse get(UUID soundId);

    Map<UUID, SoundResponse> byIds(Collection<UUID> soundIds);

    UUID use(UUID soundId);

    UUID createOriginal(UUID userId, Integer durationMs);
}
