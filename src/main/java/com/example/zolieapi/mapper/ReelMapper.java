package com.example.zolieapi.mapper;

import com.example.zolieapi.common.Enums;
import com.example.zolieapi.dto.response.ReelResponse;
import com.example.zolieapi.dto.response.UserSummaryResponse;
import com.example.zolieapi.model.Reel;
import com.example.zolieapi.repository.ReelRepository;
import com.example.zolieapi.service.MediaService;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReelMapper {

    private final ReelRepository reelRepository;
    private final MediaService mediaService;
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
        Set<UUID> liked = new HashSet<>(reelRepository.findLikedIds(viewerId, reelIds));
        Set<UUID> saved = new HashSet<>(reelRepository.findSavedIds(viewerId, reelIds));

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
                        reel.getLikeCount(),
                        reel.getCommentCount(),
                        reel.getShareCount(),
                        reel.getViewCount(),
                        liked.contains(reel.getId()),
                        saved.contains(reel.getId()),
                        reel.getAuthorId().equals(viewerId),
                        reel.getCreatedAt()))
                .toList();
    }
}
