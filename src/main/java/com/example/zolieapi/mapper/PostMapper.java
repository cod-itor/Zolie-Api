package com.example.zolieapi.mapper;

import com.example.zolieapi.common.Enums;
import com.example.zolieapi.dto.response.MediaItemResponse;
import com.example.zolieapi.dto.response.PollOptionResponse;
import com.example.zolieapi.dto.response.PollResponse;
import com.example.zolieapi.dto.response.PostResponse;
import com.example.zolieapi.dto.response.UserSummaryResponse;
import com.example.zolieapi.model.PollOption;
import com.example.zolieapi.model.Post;
import com.example.zolieapi.model.PostMedia;
import com.example.zolieapi.model.enums.PostType;
import com.example.zolieapi.repository.PollOptionRepository;
import com.example.zolieapi.repository.PollOptionRepository.VoteRow;
import com.example.zolieapi.repository.PostMediaRepository;
import com.example.zolieapi.repository.PostRepository;
import com.example.zolieapi.service.MediaService;
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

    private final PostRepository postRepository;
    private final PostMediaRepository postMediaRepository;
    private final PollOptionRepository pollOptionRepository;
    private final MediaService mediaService;
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
        Map<UUID, UUID> votes = pollOptionRepository.findVotes(viewerId, postIds).stream()
                .collect(Collectors.toMap(VoteRow::getPostId, VoteRow::getOptionId));
        Set<UUID> liked = new HashSet<>(postRepository.findLikedIds(viewerId, postIds));
        Set<UUID> saved = new HashSet<>(postRepository.findSavedIds(viewerId, postIds));

        return posts.stream()
                .map(post -> new PostResponse(
                        post.getId(),
                        Enums.value(post.getType()),
                        post.getText(),
                        Enums.value(post.getVisibility()),
                        authors.get(post.getAuthorId()),
                        toMedia(mediaByPost.getOrDefault(post.getId(), List.of()), mediaItems),
                        toPoll(post, optionsByPost.get(post.getId()), votes.get(post.getId())),
                        post.getLikeCount(),
                        post.getCommentCount(),
                        post.getShareCount(),
                        liked.contains(post.getId()),
                        saved.contains(post.getId()),
                        post.getAuthorId().equals(viewerId),
                        post.getCreatedAt(),
                        post.getUpdatedAt()))
                .toList();
    }

    public PollResponse toPoll(Post post, List<PollOption> options, UUID votedOptionId) {
        if (post.getType() != PostType.POLL || options == null) {
            return null;
        }
        int total = options.stream().mapToInt(PollOption::getVoteCount).sum();
        List<PollOptionResponse> responses = options.stream()
                .map(option -> new PollOptionResponse(option.getId(), option.getText(), option.getVoteCount(),
                        total == 0 ? 0 : Math.round(100f * option.getVoteCount() / total)))
                .toList();
        boolean ended = post.getPollEndsAt() != null && !post.getPollEndsAt().isAfter(Instant.now());
        return new PollResponse(responses, total, post.getPollEndsAt(), ended, votedOptionId);
    }

    private List<MediaItemResponse> toMedia(List<PostMedia> media, Map<UUID, MediaItemResponse> items) {
        return media.stream().map(item -> items.get(item.getMediaId())).filter(Objects::nonNull).toList();
    }
}
