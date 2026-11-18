package com.example.zolieapi.mapper;

import com.example.zolieapi.common.Enums;
import com.example.zolieapi.dto.response.MediaItemResponse;
import com.example.zolieapi.dto.response.PlaceResponse;
import com.example.zolieapi.dto.response.PollOptionResponse;
import com.example.zolieapi.dto.response.PollResponse;
import com.example.zolieapi.dto.response.PostResponse;
import com.example.zolieapi.dto.response.SoundResponse;
import com.example.zolieapi.dto.response.UserSummaryResponse;
import com.example.zolieapi.model.PollOption;
import com.example.zolieapi.model.Post;
import com.example.zolieapi.model.PostMedia;
import com.example.zolieapi.model.enums.PostType;
import com.example.zolieapi.repository.PollOptionRepository;
import com.example.zolieapi.repository.PollOptionRepository.VoteRow;
import com.example.zolieapi.repository.PostMediaRepository;
import com.example.zolieapi.repository.PostRepository;
import com.example.zolieapi.repository.PostRepository.ReactionCountRow;
import com.example.zolieapi.repository.PostRepository.ReactionRow;
import com.example.zolieapi.service.InsightService;
import com.example.zolieapi.service.MediaService;
import com.example.zolieapi.service.PlaceService;
import com.example.zolieapi.service.SoundService;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PostMapper {

    private static final int TOP_REACTIONS = 3;

    private final PostRepository postRepository;
    private final PostMediaRepository postMediaRepository;
    private final PollOptionRepository pollOptionRepository;
    private final MediaService mediaService;
    private final PlaceService placeService;
    private final SoundService soundService;
    private final InsightService insightService;
    private final UserMapper userMapper;

    public PostResponse toResponse(UUID viewerId, Post post) {
        return toResponses(viewerId, List.of(post)).get(0);
    }

    public List<PostResponse> toResponses(UUID viewerId, List<Post> posts) {
        if (posts.isEmpty()) {
            return List.of();
        }
        List<UUID> postIds = posts.stream().map(Post::getId).toList();
        Map<UUID, UserSummaryResponse> authors = userMapper.summariesById(posts.stream().map(Post::getAuthorId).toList());
        List<PostMedia> media = postMediaRepository.findByPostIds(postIds);
        Map<UUID, MediaItemResponse> mediaItems = mediaService.items(media.stream().map(PostMedia::getMediaId).toList());
        Map<UUID, List<PostMedia>> mediaByPost = media.stream().collect(Collectors.groupingBy(item -> item.getId().getPostId()));
        Map<UUID, List<PollOption>> optionsByPost = pollOptionRepository.findByPostIdInOrderByPostIdAscPositionAsc(postIds)
                .stream().collect(Collectors.groupingBy(PollOption::getPostId));
        Map<UUID, List<UUID>> votes = pollOptionRepository.findVotes(viewerId, postIds).stream()
                .collect(Collectors.groupingBy(VoteRow::getPostId, Collectors.mapping(VoteRow::getOptionId, Collectors.toList())));
        Map<UUID, String> myReactions = postRepository.findMyReactions(viewerId, postIds).stream()
                .collect(Collectors.toMap(ReactionRow::getContentId, ReactionRow::getReaction));
        Map<UUID, List<String>> topReactions = postRepository.countReactions(postIds).stream()
                .collect(Collectors.groupingBy(ReactionCountRow::getContentId,
                        Collectors.mapping(ReactionCountRow::getReaction, Collectors.toList())));
        Set<UUID> saved = new HashSet<>(postRepository.findSavedIds(viewerId, postIds));
        Map<UUID, PlaceResponse> places = placeService.byIds(posts.stream().map(Post::getPlaceId).toList());
        Map<UUID, SoundResponse> sounds = soundService.byIds(posts.stream().map(Post::getSoundId).toList());
        Map<UUID, Long> reach = insightService.reachByPost(posts.stream()
                .filter(post -> post.getAuthorId().equals(viewerId))
                .map(Post::getId)
                .toList());

        return posts.stream()
                .map(post -> {
                    boolean mine = post.getAuthorId().equals(viewerId);
                    List<UUID> myVotes = votes.getOrDefault(post.getId(), List.of());
                    return new PostResponse(
                            post.getId(),
                            Enums.value(post.getType()),
                            post.getText(),
                            Enums.value(post.getVisibility()),
                            authors.get(post.getAuthorId()),
                            toMedia(mediaByPost.getOrDefault(post.getId(), List.of()), mediaItems),
                            toPoll(post, optionsByPost.get(post.getId()), myVotes, mine),
                            places.get(post.getPlaceId()),
                            sounds.get(post.getSoundId()),
                            post.getTextBackground(),
                            post.getTextStyle(),
                            hideLikes(post, mine) ? 0 : post.getLikeCount(),
                            post.getCommentCount(),
                            post.getShareCount(),
                            mine ? reach.getOrDefault(post.getId(), 0L) : null,
                            myReactions.containsKey(post.getId()),
                            myReactions.get(post.getId()),
                            hideLikes(post, mine)
                                    ? List.of()
                                    : topReactions.getOrDefault(post.getId(), List.of()).stream().limit(TOP_REACTIONS).toList(),
                            saved.contains(post.getId()),
                            mine,
                            post.getPinnedAt() != null,
                            post.isCommentsEnabled(),
                            post.isHideLikeCount(),
                            post.getCreatedAt(),
                            post.getUpdatedAt());
                })
                .toList();
    }

    public PollResponse toPoll(Post post, List<PollOption> options, List<UUID> votedOptionIds, boolean mine) {
        if (post.getType() != PostType.POLL || options == null) {
            return null;
        }
        boolean ended = post.getPollEndsAt() != null && !post.getPollEndsAt().isAfter(Instant.now());
        boolean hidden = !post.isPollShowResultsFirst() && !mine && !ended && votedOptionIds.isEmpty();
        int total = options.stream().mapToInt(PollOption::getVoteCount).sum();
        List<PollOptionResponse> responses = options.stream()
                .map(option -> new PollOptionResponse(option.getId(), option.getText(),
                        hidden ? 0 : option.getVoteCount(),
                        hidden || total == 0 ? 0 : Math.round(100f * option.getVoteCount() / total)))
                .toList();
        return new PollResponse(responses, total, post.getPollEndsAt(), ended, post.isPollAllowMultiple(), hidden,
                votedOptionIds);
    }

    private boolean hideLikes(Post post, boolean mine) {
        return post.isHideLikeCount() && !mine;
    }

    private List<MediaItemResponse> toMedia(List<PostMedia> media, Map<UUID, MediaItemResponse> items) {
        return media.stream().map(item -> items.get(item.getMediaId())).filter(Objects::nonNull).toList();
    }
}
