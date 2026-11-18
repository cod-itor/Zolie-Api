package com.example.zolieapi.service.impl;

import com.example.zolieapi.common.Texts;
import com.example.zolieapi.dto.request.CreateSoundRequest;
import com.example.zolieapi.dto.response.SoundResponse;
import com.example.zolieapi.exception.ApiException;
import com.example.zolieapi.model.MediaAsset;
import com.example.zolieapi.model.Sound;
import com.example.zolieapi.model.User;
import com.example.zolieapi.model.enums.MediaPurpose;
import com.example.zolieapi.repository.SoundRepository;
import com.example.zolieapi.repository.UserRepository;
import com.example.zolieapi.service.MediaService;
import com.example.zolieapi.service.SoundService;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SoundServiceImpl implements SoundService {

    private static final String ORIGINAL_TITLE = "Original sound";
    private static final int MAX_TITLE = 120;
    private static final int MAX_ARTIST = 80;
    private static final int MAX_QUERY = 50;
    private static final int MAX_RESULTS = 30;

    private final SoundRepository soundRepository;
    private final UserRepository userRepository;
    private final MediaService mediaService;

    @Override
    @Transactional
    public SoundResponse create(UUID userId, CreateSoundRequest request) {
        MediaAsset audio = mediaService.requireUsable(userId, request.audioMediaId(), MediaPurpose.SOUND);
        String artist = Texts.clean(request.artistName(), MAX_ARTIST, "artistName");
        Sound sound = soundRepository.save(Sound.builder()
                .title(Texts.required(request.title(), MAX_TITLE, "title"))
                .artistName(artist != null ? artist : usernameOf(userId))
                .creatorId(userId)
                .audioMediaId(audio.getId())
                .durationMs(audio.getDurationMs())
                .build());
        return toResponses(List.of(sound)).get(0);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SoundResponse> search(String query) {
        String text = Texts.clean(query, MAX_QUERY, "q");
        String pattern = text == null ? null : Texts.likePattern(text);
        return toResponses(soundRepository.search(pattern, MAX_RESULTS));
    }

    @Override
    @Transactional(readOnly = true)
    public SoundResponse get(UUID soundId) {
        return toResponses(List.of(requireSound(soundId))).get(0);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, SoundResponse> byIds(Collection<UUID> soundIds) {
        List<UUID> ids = soundIds.stream().filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) {
            return new HashMap<>();
        }
        return toResponses(soundRepository.findAllById(ids)).stream()
                .collect(Collectors.toMap(SoundResponse::id, Function.identity()));
    }

    @Override
    @Transactional
    public UUID use(UUID soundId) {
        requireSound(soundId);
        soundRepository.addUse(soundId);
        return soundId;
    }

    @Override
    @Transactional
    public UUID createOriginal(UUID userId, Integer durationMs) {
        return soundRepository.save(Sound.builder()
                .title(ORIGINAL_TITLE)
                .artistName(usernameOf(userId))
                .creatorId(userId)
                .durationMs(durationMs)
                .useCount(1)
                .build()).getId();
    }

    private Sound requireSound(UUID soundId) {
        return soundRepository.findById(soundId).orElseThrow(() -> ApiException.rule("The sound does not exist."));
    }

    private String usernameOf(UUID userId) {
        return userRepository.findById(userId).map(User::getUsername).orElse(null);
    }

    private List<SoundResponse> toResponses(List<Sound> sounds) {
        Map<UUID, String> urls = mediaService.urls(sounds.stream().map(Sound::getAudioMediaId).toList());
        return sounds.stream()
                .map(sound -> new SoundResponse(sound.getId(), sound.getTitle(), sound.getArtistName(),
                        urls.get(sound.getAudioMediaId()), sound.getDurationMs(), sound.isLibrary(),
                        sound.getUseCount()))
                .toList();
    }
}
