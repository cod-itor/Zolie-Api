package com.example.zolieapi.mapper;

import com.example.zolieapi.common.Enums;
import com.example.zolieapi.dto.response.PlaceResponse;
import com.example.zolieapi.dto.response.ReelResponse;
import com.example.zolieapi.dto.response.SoundResponse;
import com.example.zolieapi.dto.response.UserSummaryResponse;
import com.example.zolieapi.model.Reel;
import com.example.zolieapi.repository.PostRepository.ReactionRow;
import com.example.zolieapi.repository.ReelRepository;
import com.example.zolieapi.service.MediaService;
import com.example.zolieapi.service.PlaceService;
import com.example.zolieapi.service.SoundService;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReelMapper {

    private final ReelRepository reelRepository;
    private final MediaService mediaService;
    private final PlaceService placeService;
    private final SoundService soundService;
    private final UserMapper userMapper;

    public ReelResponse toResponse(UUID viewerId, Reel reel) {
        return toResponses(viewerId, List.of(reel)).get(0);
    }

    public List<ReelResponse> toResponses(UUID viewerId, List<Reel> reels) {
        if (reels.isEmpty()) {
            return List.of();
        }
        List<UUID> reelIds = reels.stream().map(Reel::getId).toList();
        Map<UUID, UserSummaryResponse> authors = userMapper.summariesById(reels.stream().map(Reel::getAuthorId).toList());
        Map<UUID, String> urls = mediaService.urls(reels.stream()
                .flatMap(reel -> Stream.of(reel.getVideoId(), reel.getThumbnailId()))
                .toList());
        Map<UUID, String> myReactions = reelRepository.findMyReactions(viewerId, reelIds).stream()
                .collect(Collectors.toMap(ReactionRow::getContentId, ReactionRow::getReaction));
        Set<UUID> saved = new HashSet<>(reelRepository.findSavedIds(viewerId, reelIds));
        Map<UUID, PlaceResponse> places = placeService.byIds(reels.stream().map(Reel::getPlaceId).toList());
        Map<UUID, SoundResponse> sounds = soundService.byIds(reels.stream().map(Reel::getSoundId).toList());

        return reels.stream()
                .map(reel -> new ReelResponse(
                        reel.getId(),
                        reel.getCaption(),
                        Enums.value(reel.getStatus()),
                        Enums.value(reel.getVisibility()),
                        authors.get(reel.getAuthorId()),
                        urls.get(reel.getVideoId()),
                        urls.get(reel.getThumbnailId()),
                        reel.getDurationMs(),
                        places.get(reel.getPlaceId()),
                        sounds.get(reel.getSoundId()),
                        reel.getLikeCount(),
                        reel.getCommentCount(),
                        reel.getShareCount(),
                        reel.getViewCount(),
                        myReactions.containsKey(reel.getId()),
                        myReactions.get(reel.getId()),
                        saved.contains(reel.getId()),
                        reel.getAuthorId().equals(viewerId),
                        reel.isCommentsEnabled(),
                        reel.getCreatedAt()))
                .toList();
    }
}
