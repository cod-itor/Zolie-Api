package com.example.zolieapi.service.impl;

import com.example.zolieapi.common.Cursor;
import com.example.zolieapi.common.CursorPage;
import com.example.zolieapi.common.Enums;
import com.example.zolieapi.common.Reactions;
import com.example.zolieapi.common.Texts;
import com.example.zolieapi.dto.request.CreatePostRequest;
import com.example.zolieapi.dto.request.PollRequest;
import com.example.zolieapi.dto.request.UpdatePostRequest;
import com.example.zolieapi.dto.response.LikeResponse;
import com.example.zolieapi.dto.response.PollResponse;
import com.example.zolieapi.dto.response.PostResponse;
import com.example.zolieapi.dto.response.SaveResponse;
import com.example.zolieapi.dto.response.ShareResponse;
import com.example.zolieapi.dto.response.UserSummaryResponse;
import com.example.zolieapi.exception.ApiException;
import com.example.zolieapi.exception.ErrorCode;
import com.example.zolieapi.mapper.PostMapper;
import com.example.zolieapi.mapper.UserMapper;
import com.example.zolieapi.model.MediaAsset;
import com.example.zolieapi.model.PollOption;
import com.example.zolieapi.model.Post;
import com.example.zolieapi.model.PostMedia;
import com.example.zolieapi.model.enums.MediaKind;
import com.example.zolieapi.model.enums.MediaPurpose;
import com.example.zolieapi.model.enums.NotificationTarget;
import com.example.zolieapi.model.enums.NotificationType;
import com.example.zolieapi.model.enums.PostType;
import com.example.zolieapi.model.enums.Visibility;
import com.example.zolieapi.model.id.PostMediaId;
import com.example.zolieapi.repository.PollOptionRepository;
import com.example.zolieapi.repository.PollOptionRepository.VoteRow;
import com.example.zolieapi.repository.PostMediaRepository;
import com.example.zolieapi.repository.PostRepository;
import com.example.zolieapi.repository.PostRepository.ReactionRow;
import com.example.zolieapi.repository.UserRepository;
import com.example.zolieapi.security.RateLimiter;
import com.example.zolieapi.service.HashtagService;
import com.example.zolieapi.service.IdempotencyService;
import com.example.zolieapi.service.PlaceService;
import com.example.zolieapi.service.SoundService;
import com.example.zolieapi.service.MediaService;
import com.example.zolieapi.service.MentionService;
import com.example.zolieapi.service.NotificationService;
import com.example.zolieapi.service.PostService;
import com.example.zolieapi.service.VisibilityService;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {

    private static final String RESOURCE = "post";
    private static final int MAX_CAPTION = 2200;
    private static final int MAX_POLL_OPTION = 80;
    private static final int MIN_POLL_OPTIONS = 2;
    private static final int MAX_POLL_OPTIONS = 4;
    private static final int MIN_CAROUSEL_ITEMS = 2;
    private static final int MAX_CAROUSEL_ITEMS = 10;
    private static final int MAX_PINNED = 3;

    private final PostRepository postRepository;
    private final PostMediaRepository postMediaRepository;
    private final PollOptionRepository pollOptionRepository;
    private final UserRepository userRepository;
    private final MediaService mediaService;
    private final MentionService mentionService;
    private final HashtagService hashtagService;
    private final SoundService soundService;
    private final PlaceService placeService;
    private final NotificationService notificationService;
    private final VisibilityService visibilityService;
    private final IdempotencyService idempotencyService;
    private final RateLimiter rateLimiter;
    private final PostMapper postMapper;
    private final UserMapper userMapper;

    @Override
    @Transactional
    public PostResponse create(UUID userId, UUID idempotencyKey, CreatePostRequest request) {
        Optional<UUID> existing = idempotencyService.find(userId, idempotencyKey, RESOURCE);
        if (existing.isPresent()) {
            return get(userId, existing.get());
        }
        rateLimiter.check("post", userId, 30, Duration.ofHours(1));

        PostType type = Enums.require(PostType.class, request.type(), "type");
        String caption = Texts.clean(request.caption(), MAX_CAPTION, "caption");
        List<UUID> mediaIds = request.mediaIds() == null ? List.of() : request.mediaIds().stream().distinct().toList();
        List<MediaAsset> media = mediaIds.stream()
                .map(mediaId -> mediaService.requireUsable(userId, mediaId, MediaPurpose.POST))
                .toList();
        validateContent(type, caption, media, request.poll());

        Post post = postRepository.save(Post.builder()
                .authorId(userId)
                .type(type)
                .text(caption)
                .visibility(parseVisibility(request.visibility()))
                .pollEndsAt(type == PostType.POLL ? request.poll().endsAt() : null)
                .pollAllowMultiple(type == PostType.POLL && Boolean.TRUE.equals(request.poll().allowMultiple()))
                .pollShowResultsFirst(type != PostType.POLL || !Boolean.FALSE.equals(request.poll().showResultsBeforeVoting()))
                .placeId(requirePlace(request.placeId()))
                .soundId(request.soundId() == null ? null : soundService.use(request.soundId()))
                .textBackground(Texts.clean(request.textBackground()))
                .textStyle(Texts.clean(request.textStyle()))
                .commentsEnabled(!Boolean.FALSE.equals(request.commentsEnabled()))
                .build());
        attachMedia(post.getId(), mediaIds);
        if (type == PostType.POLL) {
            savePollOptions(post.getId(), request.poll().options());
        }
        userRepository.addPostCount(userId, 1);
        hashtagService.tagPost(post.getId(), caption);
        mentionService.mention(userId, caption, request.taggedUserIds(), NotificationTarget.POST, post.getId());
        idempotencyService.remember(userId, idempotencyKey, RESOURCE, post.getId());
        return postMapper.toResponse(userId, post);
    }

    @Override
    @Transactional(readOnly = true)
    public PostResponse get(UUID viewerId, UUID postId) {
        return postMapper.toResponse(viewerId, requireVisible(viewerId, postId));
    }

    @Override
    @Transactional
    public PostResponse update(UUID userId, UUID postId, UpdatePostRequest request) {
        Post post = requireOwn(userId, postId);
        if (request.caption() != null) {
            String caption = Texts.clean(request.caption(), MAX_CAPTION, "caption");
            if (caption == null && (post.getType() == PostType.TEXT || post.getType() == PostType.POLL)) {
                throw ApiException.rule("This post needs text.");
            }
            post.setText(caption);
            hashtagService.tagPost(postId, caption);
        }
        if (request.visibility() != null) {
            post.setVisibility(parseVisibility(request.visibility()));
        }
        if (request.placeId() != null) {
            post.setPlaceId(requirePlace(request.placeId()));
        }
        if (request.commentsEnabled() != null) {
            post.setCommentsEnabled(request.commentsEnabled());
        }
        if (request.hideLikeCount() != null) {
            post.setHideLikeCount(request.hideLikeCount());
        }
        if (request.pinned() != null) {
            changePin(post, request.pinned());
        }
        if (request.caption() != null || request.taggedUserIds() != null) {
            mentionService.replaceForPost(userId, postId, post.getText(), request.taggedUserIds());
        }
        return postMapper.toResponse(userId, postRepository.saveAndFlush(post));
    }

    @Override
    @Transactional
    public void delete(UUID userId, UUID postId) {
        Post post = requireOwn(userId, postId);
        post.setDeletedAt(Instant.now());
        postRepository.saveAndFlush(post);
        postRepository.deleteMedia(postId);
        userRepository.addPostCount(userId, -1);
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPage<PostResponse> feed(UUID viewerId, String cursor, Integer limit) {
        int size = CursorPage.limit(limit);
        Cursor from = Cursor.decode(cursor);
        return page(viewerId, postRepository.findFeed(viewerId, from.time(), from.id(), size + 1), size);
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPage<PostResponse> listByAuthor(UUID viewerId, UUID authorId, String cursor, Integer limit) {
        visibilityService.requireCanSeeContentOf(viewerId, authorId);
        int size = CursorPage.limit(limit);
        Cursor from = Cursor.decode(cursor);
        return page(viewerId, postRepository.findByAuthor(authorId, viewerId, from.time(), from.id(), size + 1), size);
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPage<PostResponse> listSaved(UUID userId, String cursor, Integer limit) {
        int size = CursorPage.limit(limit);
        Cursor from = Cursor.decode(cursor);
        return page(userId, postRepository.findSaved(userId, from.time(), from.id(), size + 1), size);
    }

    @Override
    @Transactional
    public LikeResponse like(UUID userId, UUID postId, String reaction) {
        rateLimiter.check("like", userId, 120, Duration.ofMinutes(1));
        String emoji = Reactions.parse(reaction);
        Post post = requireVisible(userId, postId);
        if (postRepository.insertLike(postId, userId, emoji) == 1) {
            postRepository.addLikeCount(postId, 1);
            notificationService.send(post.getAuthorId(), userId, NotificationType.POST_LIKE, NotificationTarget.POST, postId);
        } else if (reaction != null && !reaction.isBlank()) {
            postRepository.updateReaction(postId, userId, emoji);
        }
        String current = postRepository.findMyReactions(userId, Set.of(postId)).stream()
                .map(ReactionRow::getReaction).findFirst().orElse(emoji);
        return new LikeResponse(true, current, visibleLikeCount(userId, post));
    }

    @Override
    @Transactional
    public LikeResponse unlike(UUID userId, UUID postId) {
        if (!postRepository.existsById(postId)) {
            throw ApiException.notFound();
        }
        if (postRepository.deleteLike(postId, userId) == 1) {
            postRepository.addLikeCount(postId, -1);
        }
        long likeCount = postRepository.findVisible(postId, userId)
                .map(post -> visibleLikeCount(userId, post))
                .orElse(0L);
        return new LikeResponse(false, null, likeCount);
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPage<UserSummaryResponse> listLikers(UUID viewerId, UUID postId, String cursor, Integer limit) {
        Post post = requireVisible(viewerId, postId);
        if (post.isHideLikeCount() && !post.getAuthorId().equals(viewerId)) {
            throw ApiException.forbidden();
        }
        int size = CursorPage.limit(limit);
        return CursorPage.of(userRepository.findPostLikers(postId, viewerId, Cursor.decode(cursor).id(), size + 1),
                size, user -> Cursor.ofId(user.getId()), userMapper::toSummaries);
    }

    @Override
    @Transactional
    public SaveResponse save(UUID userId, UUID postId) {
        requireVisible(userId, postId);
        postRepository.insertSave(postId, userId);
        return new SaveResponse(true);
    }

    @Override
    @Transactional
    public SaveResponse unsave(UUID userId, UUID postId) {
        postRepository.deleteSave(postId, userId);
        return new SaveResponse(false);
    }

    @Override
    @Transactional
    public ShareResponse share(UUID userId, UUID postId) {
        rateLimiter.check("share", userId, 60, Duration.ofMinutes(1));
        requireVisible(userId, postId);
        postRepository.addShare(postId);
        return new ShareResponse(postRepository.findShareCount(postId));
    }

    @Override
    @Transactional
    public void hide(UUID userId, UUID postId) {
        requireVisible(userId, postId);
        postRepository.hide(postId, userId);
    }

    @Override
    @Transactional
    public void unhide(UUID userId, UUID postId) {
        postRepository.unhide(postId, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PostResponse> listPinned(UUID viewerId, UUID authorId) {
        visibilityService.requireCanSeeContentOf(viewerId, authorId);
        return postMapper.toResponses(viewerId, postRepository.findPinned(authorId, viewerId));
    }

    @Override
    @Transactional
    public PollResponse vote(UUID userId, UUID postId, UUID optionId) {
        Post post = requireVisible(userId, postId);
        if (post.getType() != PostType.POLL) {
            throw ApiException.rule("This post is not a poll.");
        }
        if (post.getPollEndsAt() != null && !post.getPollEndsAt().isAfter(Instant.now())) {
            throw new ApiException(ErrorCode.POLL_ENDED);
        }
        boolean belongsToPoll = pollOptionRepository.findById(optionId)
                .filter(option -> option.getPostId().equals(postId))
                .isPresent();
        if (!belongsToPoll) {
            throw ApiException.rule("This option does not belong to the poll.");
        }
        pollOptionRepository.lock("vote:" + postId + ":" + userId);
        boolean alreadyVoted = !post.isPollAllowMultiple() && pollOptionRepository.hasVoted(postId, userId);
        if (alreadyVoted || pollOptionRepository.insertVote(postId, userId, optionId) == 0) {
            throw new ApiException(ErrorCode.ALREADY_VOTED);
        }
        pollOptionRepository.addVote(optionId);
        List<PollOption> options = pollOptionRepository.findByPostIdInOrderByPostIdAscPositionAsc(Set.of(postId));
        List<UUID> voted = pollOptionRepository.findVotes(userId, Set.of(postId)).stream().map(VoteRow::getOptionId).toList();
        return postMapper.toPoll(post, options, voted, post.getAuthorId().equals(userId));
    }

    private void validateContent(PostType type, String caption, List<MediaAsset> media, PollRequest poll) {
        if (type != PostType.POLL && poll != null) {
            throw ApiException.rule("Only poll posts can have a poll.");
        }
        switch (type) {
            case TEXT -> {
                requireCaption(caption);
                requireNoMedia(media);
            }
            case POLL -> {
                requireCaption(caption);
                requireNoMedia(media);
                validatePoll(poll);
            }
            case PHOTO -> requireSingle(media, MediaKind.IMAGE, "A photo post needs exactly one image.");
            case VIDEO -> requireSingle(media, MediaKind.VIDEO, "A video post needs exactly one video.");
            case AUDIO -> requireSingle(media, MediaKind.AUDIO, "An audio post needs exactly one audio file.");
            case CAROUSEL -> validateCarousel(media);
        }
    }

    private void requireCaption(String caption) {
        if (caption == null) {
            throw ApiException.rule("caption is required for this post type.");
        }
    }

    private void requireNoMedia(List<MediaAsset> media) {
        if (!media.isEmpty()) {
            throw ApiException.rule("This post type cannot have media.");
        }
    }

    private void requireSingle(List<MediaAsset> media, MediaKind kind, String message) {
        if (media.size() != 1 || media.get(0).getKind() != kind) {
            throw ApiException.rule(message);
        }
    }

    private void validateCarousel(List<MediaAsset> media) {
        if (media.size() < MIN_CAROUSEL_ITEMS || media.size() > MAX_CAROUSEL_ITEMS) {
            throw ApiException.rule("A carousel needs 2 to 10 photos or videos.");
        }
        if (media.stream().anyMatch(item -> item.getKind() == MediaKind.AUDIO)) {
            throw ApiException.rule("A carousel can only have photos and videos.");
        }
    }

    private void validatePoll(PollRequest poll) {
        if (poll == null) {
            throw ApiException.rule("A poll post needs poll options.");
        }
        List<String> options = poll.options().stream().map(Texts::clean).toList();
        boolean invalidCount = options.size() < MIN_POLL_OPTIONS || options.size() > MAX_POLL_OPTIONS;
        if (invalidCount || options.contains(null) || options.stream().distinct().count() != options.size()) {
            throw ApiException.rule("A poll needs 2 to 4 different options.");
        }
        options.forEach(option -> Texts.clean(option, MAX_POLL_OPTION, "poll option"));
        if (poll.endsAt() != null && !poll.endsAt().isAfter(Instant.now())) {
            throw ApiException.rule("The poll end time must be in the future.");
        }
    }

    private long visibleLikeCount(UUID viewerId, Post post) {
        if (post.isHideLikeCount() && !post.getAuthorId().equals(viewerId)) {
            return 0;
        }
        return postRepository.findLikeCount(post.getId());
    }

    private void changePin(Post post, boolean pinned) {
        if (!pinned) {
            post.setPinnedAt(null);
            return;
        }
        if (post.getPinnedAt() == null && postRepository.countPinned(post.getAuthorId()) >= MAX_PINNED) {
            throw ApiException.rule("You can pin up to " + MAX_PINNED + " posts.");
        }
        post.setPinnedAt(Instant.now());
    }

    private UUID requirePlace(UUID placeId) {
        placeService.requireExists(placeId);
        return placeId;
    }

    private Visibility parseVisibility(String visibility) {
        return visibility == null ? Visibility.PUBLIC : Enums.parse(Visibility.class, visibility, "visibility");
    }

    private void attachMedia(UUID postId, List<UUID> mediaIds) {
        for (int position = 0; position < mediaIds.size(); position++) {
            postMediaRepository.save(PostMedia.builder()
                    .id(new PostMediaId(postId, position))
                    .mediaId(mediaIds.get(position))
                    .build());
        }
    }

    private void savePollOptions(UUID postId, List<String> options) {
        for (int position = 0; position < options.size(); position++) {
            pollOptionRepository.save(PollOption.builder()
                    .postId(postId)
                    .position(position)
                    .text(Texts.clean(options.get(position)))
                    .build());
        }
    }

    private Post requireVisible(UUID viewerId, UUID postId) {
        return postRepository.findVisible(postId, viewerId).orElseThrow(ApiException::notFound);
    }

    private Post requireOwn(UUID userId, UUID postId) {
        Post post = requireVisible(userId, postId);
        if (!post.getAuthorId().equals(userId)) {
            throw ApiException.forbidden();
        }
        return post;
    }

    private CursorPage<PostResponse> page(UUID viewerId, List<Post> posts, int size) {
        return CursorPage.of(posts, size, post -> new Cursor(post.getCreatedAt(), post.getId()),
                page -> postMapper.toResponses(viewerId, page));
    }
}
