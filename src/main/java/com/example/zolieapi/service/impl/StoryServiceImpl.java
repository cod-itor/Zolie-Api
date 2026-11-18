package com.example.zolieapi.service.impl;

import com.example.zolieapi.common.Cursor;
import com.example.zolieapi.common.CursorPage;
import com.example.zolieapi.common.Enums;
import com.example.zolieapi.common.Texts;
import com.example.zolieapi.dto.request.CreateStoryRequest;
import com.example.zolieapi.dto.request.StickerAnswerRequest;
import com.example.zolieapi.dto.request.StickerRequest;
import com.example.zolieapi.dto.response.MediaItemResponse;
import com.example.zolieapi.dto.response.MessageResponse;
import com.example.zolieapi.dto.response.StickerAnswerResponse;
import com.example.zolieapi.dto.response.StickerAnswersResponse;
import com.example.zolieapi.dto.response.StickerResponse;
import com.example.zolieapi.dto.response.StoryResponse;
import com.example.zolieapi.dto.response.StoryRingResponse;
import com.example.zolieapi.dto.response.UserSummaryResponse;
import com.example.zolieapi.exception.ApiException;
import com.example.zolieapi.exception.ErrorCode;
import com.example.zolieapi.mapper.MessageMapper;
import com.example.zolieapi.mapper.PostMapper;
import com.example.zolieapi.mapper.UserMapper;
import com.example.zolieapi.dto.response.PostResponse;
import com.example.zolieapi.model.MediaAsset;
import com.example.zolieapi.model.Post;
import com.example.zolieapi.security.Viewer;
import com.example.zolieapi.model.Message;
import com.example.zolieapi.model.Story;
import com.example.zolieapi.model.StorySticker;
import com.example.zolieapi.model.User;
import com.example.zolieapi.model.StoryStickerResponse;
import com.example.zolieapi.model.enums.MediaKind;
import com.example.zolieapi.model.enums.MediaPurpose;
import com.example.zolieapi.model.enums.MessageType;
import com.example.zolieapi.model.enums.NotificationTarget;
import com.example.zolieapi.model.enums.NotificationType;
import com.example.zolieapi.model.enums.StickerType;
import com.example.zolieapi.model.enums.StoryAudience;
import com.example.zolieapi.repository.PostRepository;
import com.example.zolieapi.repository.StoryRepository;
import com.example.zolieapi.repository.StoryRepository.RingRow;
import com.example.zolieapi.repository.StoryStickerRepository;
import com.example.zolieapi.repository.StoryStickerResponseRepository;
import com.example.zolieapi.repository.UserRepository;
import com.example.zolieapi.security.RateLimiter;
import com.example.zolieapi.service.ConversationService;
import com.example.zolieapi.service.IdempotencyService;
import com.example.zolieapi.service.MediaService;
import com.example.zolieapi.service.NotificationService;
import com.example.zolieapi.service.StoryService;
import com.example.zolieapi.service.VisibilityService;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.stream.Stream;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StoryServiceImpl implements StoryService {

    private static final String RESOURCE = "story";
    private static final Duration LIFETIME = Duration.ofHours(24);
    private static final int MAX_PROMPT = 100;
    private static final int MAX_EMOJI = 16;
    private static final int MAX_ANSWER = 300;
    private static final int MAX_REPLY = 2000;
    private static final int MAX_TEXT = 500;

    private final StoryRepository storyRepository;
    private final StoryStickerRepository stickerRepository;
    private final StoryStickerResponseRepository responseRepository;
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final PostMapper postMapper;
    private final MediaService mediaService;
    private final ConversationService conversationService;
    private final NotificationService notificationService;
    private final VisibilityService visibilityService;
    private final IdempotencyService idempotencyService;
    private final RateLimiter rateLimiter;
    private final UserMapper userMapper;
    private final MessageMapper messageMapper;

    @Override
    @Transactional
    public StoryResponse create(UUID userId, UUID idempotencyKey, CreateStoryRequest request) {
        Optional<UUID> existing = idempotencyService.find(userId, idempotencyKey, RESOURCE);
        if (existing.isPresent()) {
            return toResponses(userId, List.of(requireVisible(userId, existing.get()))).get(0);
        }
        rateLimiter.check("story", userId, 30, Duration.ofHours(1));
        String text = Texts.clean(request.text(), MAX_TEXT, "text");
        requireSingleContent(request, text);
        if (request.mediaId() != null) {
            MediaAsset media = mediaService.requireUsable(userId, request.mediaId(), MediaPurpose.STORY);
            if (media.getKind() == MediaKind.AUDIO) {
                throw ApiException.rule("A story needs a photo or a video.");
            }
        }
        if (request.sharedPostId() != null) {
            requireShareablePost(userId, request.sharedPostId());
        }
        List<StickerRequest> stickers = request.stickers() == null ? List.of() : request.stickers();
        validateStickers(stickers);

        Instant now = Instant.now();
        Story story = storyRepository.save(Story.builder()
                .authorId(userId)
                .mediaId(request.mediaId())
                .textContent(text)
                .textBackground(Texts.clean(request.textBackground()))
                .sharedPostId(request.sharedPostId())
                .audience(request.audience() == null
                        ? StoryAudience.EVERYONE
                        : Enums.parse(StoryAudience.class, request.audience(), "audience"))
                .createdAt(now)
                .expiresAt(now.plus(LIFETIME))
                .build());
        stickers.forEach(sticker -> stickerRepository.save(toSticker(story.getId(), sticker)));
        idempotencyService.remember(userId, idempotencyKey, RESOURCE, story.getId());
        return toResponses(userId, List.of(story)).get(0);
    }

    @Override
    @Transactional
    public void delete(UUID userId, UUID storyId) {
        Story story = requireOwn(userId, storyId);
        storyRepository.delete(story);
        storyRepository.flush();
        mediaService.deleteAll(Arrays.asList(story.getMediaId()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<StoryRingResponse> feed(UUID userId) {
        List<RingRow> rings = storyRepository.findRings(userId);
        Map<UUID, UserSummaryResponse> users = userMapper.summariesById(rings.stream().map(RingRow::getUserId).toList());
        return rings.stream()
                .map(ring -> new StoryRingResponse(users.get(ring.getUserId()), Boolean.TRUE.equals(ring.getHasUnseen()),
                        Boolean.TRUE.equals(ring.getCloseFriends()), ring.getStoryCount()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<StoryResponse> listByAuthor(UUID viewerId, UUID authorId) {
        visibilityService.requireCanSeeContentOf(viewerId, authorId);
        return toResponses(viewerId, storyRepository.findActiveByAuthor(authorId, viewerId));
    }

    @Override
    @Transactional
    public void markSeen(UUID viewerId, UUID storyId) {
        Story story = requireVisible(viewerId, storyId);
        if (!story.getAuthorId().equals(viewerId) && storyRepository.insertView(storyId, viewerId) == 1) {
            storyRepository.addView(storyId);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPage<UserSummaryResponse> listViewers(UUID userId, UUID storyId, String cursor, Integer limit) {
        requireOwn(userId, storyId);
        int size = CursorPage.limit(limit);
        return CursorPage.of(userRepository.findStoryViewers(storyId, Cursor.decode(cursor).id(), size + 1), size,
                user -> Cursor.ofId(user.getId()), userMapper::toSummaries);
    }

    @Override
    @Transactional
    public MessageResponse reply(UUID userId, UUID storyId, String text) {
        rateLimiter.check("message", userId, 30, Duration.ofMinutes(1));
        Story story = requireVisibleToOthers(userId, storyId);
        requireAcceptsReplies(story.getAuthorId(), userId);
        Message message = conversationService.deliverDirect(userId, story.getAuthorId(), MessageType.STORY_REPLY,
                Texts.required(text, MAX_REPLY, "text"), storyId);
        notificationService.send(story.getAuthorId(), userId, NotificationType.STORY_REPLY, NotificationTarget.STORY, storyId);
        return messageMapper.toResponse(message);
    }

    @Override
    @Transactional
    public MessageResponse answerSticker(UUID userId, UUID storyId, UUID stickerId, StickerAnswerRequest request) {
        rateLimiter.check("message", userId, 30, Duration.ofMinutes(1));
        Story story = requireVisibleToOthers(userId, storyId);
        requireAcceptsReplies(story.getAuthorId(), userId);
        StorySticker sticker = requireSticker(storyId, stickerId);
        if (responseRepository.findByStickerIdAndResponderId(stickerId, userId).isPresent()) {
            throw new ApiException(ErrorCode.ALREADY_ANSWERED);
        }
        StoryStickerResponse response = StoryStickerResponse.builder().stickerId(stickerId).responderId(userId).build();
        String messageText = switch (sticker.getType()) {
            case QUESTION -> {
                response.setAnswerText(Texts.required(request.text(), MAX_ANSWER, "text"));
                yield response.getAnswerText();
            }
            case EMOJI_SLIDER -> {
                if (request.value() == null || request.value() < 0 || request.value() > 100) {
                    throw ApiException.rule("value must be between 0 and 100.");
                }
                response.setSliderValue(request.value());
                yield sticker.getEmoji() + " " + request.value() + "%";
            }
            case COUNTDOWN -> throw ApiException.rule("A countdown sticker has no answer. Use the reminder instead.");
        };
        Message message = conversationService.deliverDirect(userId, story.getAuthorId(), MessageType.STORY_REPLY,
                messageText, storyId);
        response.setMessageId(message.getId());
        responseRepository.save(response);
        stickerRepository.addResponse(stickerId);
        notificationService.send(story.getAuthorId(), userId, NotificationType.STICKER_RESPONSE,
                NotificationTarget.STORY_STICKER, stickerId);
        return messageMapper.toResponse(message);
    }

    @Override
    @Transactional(readOnly = true)
    public StickerAnswersResponse listStickerAnswers(UUID userId, UUID storyId, UUID stickerId, String cursor,
                                                     Integer limit) {
        requireOwn(userId, storyId);
        StorySticker sticker = requireSticker(storyId, stickerId);
        int size = CursorPage.limit(limit);
        Cursor from = Cursor.decode(cursor);
        CursorPage<StickerAnswerResponse> page = CursorPage.of(
                responseRepository.findAnswers(stickerId, from.time(), from.id(), size + 1), size,
                response -> new Cursor(response.getCreatedAt(), response.getId()), this::toAnswers);
        return new StickerAnswersResponse(page.items(), page.nextCursor(), sticker.getResponseCount(),
                responseRepository.averageSlider(stickerId));
    }

    @Override
    @Transactional
    public void setReminder(UUID userId, UUID storyId, UUID stickerId, boolean remind) {
        requireVisible(userId, storyId);
        StorySticker sticker = requireSticker(storyId, stickerId);
        if (sticker.getType() != StickerType.COUNTDOWN) {
            throw ApiException.rule("Only countdown stickers have reminders.");
        }
        Optional<StoryStickerResponse> existing = responseRepository.findByStickerIdAndResponderId(stickerId, userId);
        if (remind && existing.isEmpty()) {
            responseRepository.save(StoryStickerResponse.builder()
                    .stickerId(stickerId)
                    .responderId(userId)
                    .remindMe(true)
                    .build());
        }
        if (!remind && existing.isPresent()) {
            responseRepository.delete(existing.get());
        }
    }

    private void requireAcceptsReplies(UUID authorId, UUID userId) {
        User author = visibilityService.requireActiveUser(authorId);
        if (!visibilityService.allows(author.getStoryRepliesFrom(), authorId, userId)) {
            throw new ApiException(ErrorCode.FORBIDDEN, "This person does not accept replies from you.");
        }
    }

    private void requireSingleContent(CreateStoryRequest request, String text) {
        long parts = Stream.of(request.mediaId(), text, request.sharedPostId()).filter(Objects::nonNull).count();
        if (parts != 1) {
            throw ApiException.rule("A moment needs exactly one of: mediaId, text or sharedPostId.");
        }
    }

    private void requireShareablePost(UUID userId, UUID postId) {
        Post post = postRepository.findVisible(postId, userId).orElseThrow(() -> ApiException.rule("The post was not found."));
        boolean publicPost = postRepository.findVisible(postId, Viewer.GUEST).isPresent();
        if (!post.getAuthorId().equals(userId) && !publicPost) {
            throw ApiException.rule("Only public posts can be shared as a moment.");
        }
    }

    private void validateStickers(List<StickerRequest> stickers) {
        Set<StickerType> seen = new HashSet<>();
        for (StickerRequest sticker : stickers) {
            StickerType type = Enums.require(StickerType.class, sticker.type(), "sticker type");
            if (!seen.add(type)) {
                throw ApiException.rule("A story can have at most one sticker of each type.");
            }
            switch (type) {
                case QUESTION -> Texts.required(sticker.prompt(), MAX_PROMPT, "sticker prompt");
                case EMOJI_SLIDER -> Texts.required(sticker.emoji(), MAX_EMOJI, "sticker emoji");
                case COUNTDOWN -> {
                    Texts.required(sticker.prompt(), MAX_PROMPT, "sticker prompt");
                    if (sticker.endsAt() == null || !sticker.endsAt().isAfter(Instant.now())) {
                        throw ApiException.rule("The countdown must end in the future.");
                    }
                }
            }
            if (isOutsideUnit(sticker.x()) || isOutsideUnit(sticker.y())) {
                throw ApiException.rule("Sticker x and y must be between 0 and 1.");
            }
        }
    }

    private boolean isOutsideUnit(Float value) {
        return value != null && (value < 0 || value > 1);
    }

    private StorySticker toSticker(UUID storyId, StickerRequest request) {
        StickerType type = Enums.require(StickerType.class, request.type(), "sticker type");
        StorySticker sticker = StorySticker.builder()
                .storyId(storyId)
                .type(type)
                .prompt(Texts.clean(request.prompt()))
                .emoji(Texts.clean(request.emoji()))
                .countdownEndsAt(type == StickerType.COUNTDOWN ? request.endsAt() : null)
                .build();
        if (request.x() != null) {
            sticker.setPosX(request.x());
        }
        if (request.y() != null) {
            sticker.setPosY(request.y());
        }
        if (request.scale() != null) {
            sticker.setScale(request.scale());
        }
        if (request.rotation() != null) {
            sticker.setRotationDeg(request.rotation());
        }
        return sticker;
    }

    private Story requireVisible(UUID viewerId, UUID storyId) {
        return storyRepository.findVisible(storyId, viewerId).orElseThrow(ApiException::notFound);
    }

    private Story requireVisibleToOthers(UUID viewerId, UUID storyId) {
        Story story = requireVisible(viewerId, storyId);
        if (story.getAuthorId().equals(viewerId)) {
            throw ApiException.rule("You cannot reply to your own story.");
        }
        return story;
    }

    private Story requireOwn(UUID userId, UUID storyId) {
        Story story = requireVisible(userId, storyId);
        if (!story.getAuthorId().equals(userId)) {
            throw ApiException.forbidden();
        }
        return story;
    }

    private StorySticker requireSticker(UUID storyId, UUID stickerId) {
        return stickerRepository.findById(stickerId)
                .filter(sticker -> sticker.getStoryId().equals(storyId))
                .orElseThrow(ApiException::notFound);
    }

    private List<StickerAnswerResponse> toAnswers(List<StoryStickerResponse> responses) {
        Map<UUID, UserSummaryResponse> users = userMapper.summariesById(
                responses.stream().map(StoryStickerResponse::getResponderId).toList());
        return responses.stream()
                .map(response -> new StickerAnswerResponse(users.get(response.getResponderId()),
                        response.getAnswerText(), response.getSliderValue(), response.getCreatedAt()))
                .toList();
    }

    private List<StoryResponse> toResponses(UUID viewerId, List<Story> stories) {
        if (stories.isEmpty()) {
            return List.of();
        }
        List<UUID> storyIds = stories.stream().map(Story::getId).toList();
        Map<UUID, UserSummaryResponse> authors = userMapper.summariesById(stories.stream().map(Story::getAuthorId).toList());
        Map<UUID, MediaItemResponse> media = mediaService.items(stories.stream().map(Story::getMediaId).toList());
        Set<UUID> seen = new HashSet<>(storyRepository.findSeenIds(viewerId, storyIds));
        List<StorySticker> stickers = stickerRepository.findByStoryIdInOrderByCreatedAtAsc(storyIds);
        Map<UUID, StoryStickerResponse> myResponses = responseRepository
                .findByResponderIdAndStickerIdIn(viewerId, stickers.stream().map(StorySticker::getId).toList())
                .stream().collect(Collectors.toMap(StoryStickerResponse::getStickerId, Function.identity()));
        Map<UUID, List<StorySticker>> stickersByStory = stickers.stream().collect(Collectors.groupingBy(StorySticker::getStoryId));
        List<Post> sharedPosts = stories.stream().map(Story::getSharedPostId).filter(Objects::nonNull).distinct()
                .map(postId -> postRepository.findVisible(postId, viewerId)).flatMap(Optional::stream).toList();
        Map<UUID, PostResponse> sharedById = postMapper.toResponses(viewerId, sharedPosts).stream()
                .collect(Collectors.toMap(PostResponse::id, Function.identity()));

        return stories.stream()
                .map(story -> {
                    boolean mine = story.getAuthorId().equals(viewerId);
                    List<StickerResponse> stickerResponses = stickersByStory.getOrDefault(story.getId(), List.of()).stream()
                            .map(sticker -> toStickerResponse(sticker, myResponses.get(sticker.getId()), mine))
                            .toList();
                    return new StoryResponse(story.getId(), authors.get(story.getAuthorId()), media.get(story.getMediaId()),
                            story.getTextContent(), story.getTextBackground(), sharedById.get(story.getSharedPostId()),
                            Enums.value(story.getAudience()), mine || seen.contains(story.getId()), mine,
                            mine ? story.getViewCount() : null, stickerResponses, story.getCreatedAt(), story.getExpiresAt());
                })
                .toList();
    }

    private StickerResponse toStickerResponse(StorySticker sticker, StoryStickerResponse mine, boolean isAuthor) {
        boolean answered = mine != null && (mine.getAnswerText() != null || mine.getSliderValue() != null);
        boolean reminderSet = mine != null && mine.isRemindMe();
        return new StickerResponse(sticker.getId(), Enums.value(sticker.getType()), sticker.getPrompt(), sticker.getEmoji(),
                sticker.getCountdownEndsAt(), sticker.getPosX(), sticker.getPosY(), sticker.getScale(),
                sticker.getRotationDeg(), answered, reminderSet, isAuthor ? sticker.getResponseCount() : null);
    }
}
