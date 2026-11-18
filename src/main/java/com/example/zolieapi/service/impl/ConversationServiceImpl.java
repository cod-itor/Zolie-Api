package com.example.zolieapi.service.impl;

import com.example.zolieapi.common.Cursor;
import com.example.zolieapi.common.CursorPage;
import com.example.zolieapi.common.Enums;
import com.example.zolieapi.common.Pairs;
import com.example.zolieapi.common.Texts;
import com.example.zolieapi.dto.request.SendMessageRequest;
import com.example.zolieapi.dto.response.ConversationResponse;
import com.example.zolieapi.dto.response.MessageResponse;
import com.example.zolieapi.dto.response.ReadReceiptResponse;
import com.example.zolieapi.dto.response.UserSummaryResponse;
import com.example.zolieapi.exception.ApiException;
import com.example.zolieapi.exception.ErrorCode;
import com.example.zolieapi.mapper.MessageMapper;
import com.example.zolieapi.mapper.UserMapper;
import com.example.zolieapi.model.Conversation;
import com.example.zolieapi.model.ConversationParticipant;
import com.example.zolieapi.model.Match;
import com.example.zolieapi.model.Message;
import com.example.zolieapi.model.enums.ConversationType;
import com.example.zolieapi.model.enums.MatchStatus;
import com.example.zolieapi.model.enums.MediaPurpose;
import com.example.zolieapi.model.enums.MessageType;
import com.example.zolieapi.model.enums.ParticipantState;
import com.example.zolieapi.model.id.ParticipantId;
import com.example.zolieapi.repository.ConversationParticipantRepository;
import com.example.zolieapi.repository.ConversationRepository;
import com.example.zolieapi.repository.MatchRepository;
import com.example.zolieapi.repository.MessageRepository;
import com.example.zolieapi.repository.MessageRepository.UnreadRow;
import com.example.zolieapi.repository.PostRepository;
import com.example.zolieapi.repository.ReelRepository;
import com.example.zolieapi.security.RateLimiter;
import com.example.zolieapi.service.ConversationService;
import com.example.zolieapi.service.MediaService;
import com.example.zolieapi.service.VisibilityService;
import com.example.zolieapi.service.event.ConversationReadEvent;
import com.example.zolieapi.service.event.MessageSentEvent;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ConversationServiceImpl implements ConversationService {

    private static final Set<String> FOLDERS = Set.of("inbox", "requests", "dating");
    private static final int MAX_TEXT = 2000;
    private static final int DEFAULT_MESSAGE_PAGE = 30;

    private final ConversationRepository conversationRepository;
    private final ConversationParticipantRepository participantRepository;
    private final MessageRepository messageRepository;
    private final MatchRepository matchRepository;
    private final PostRepository postRepository;
    private final ReelRepository reelRepository;
    private final VisibilityService visibilityService;
    private final MediaService mediaService;
    private final RateLimiter rateLimiter;
    private final ApplicationEventPublisher eventPublisher;
    private final MessageMapper messageMapper;
    private final UserMapper userMapper;

    @Override
    @Transactional(readOnly = true)
    public CursorPage<ConversationResponse> list(UUID userId, String folder, String cursor, Integer limit) {
        String normalized = folder == null ? "inbox" : folder.toLowerCase();
        if (!FOLDERS.contains(normalized)) {
            throw ApiException.invalid("folder must be one of: inbox, requests, dating");
        }
        int size = CursorPage.limit(limit);
        Cursor from = Cursor.decode(cursor);
        List<Conversation> conversations =
                conversationRepository.findForUser(userId, normalized, from.time(), from.id(), size + 1);
        return CursorPage.of(conversations, size,
                conversation -> new Cursor(sortTime(conversation), conversation.getId()),
                page -> toResponses(userId, page));
    }

    @Override
    @Transactional
    public ConversationResponse open(UUID userId, UUID otherUserId) {
        if (userId.equals(otherUserId)) {
            throw ApiException.rule("You cannot message yourself.");
        }
        visibilityService.requireReachableUser(userId, otherUserId);
        Conversation conversation = findOrCreateRegular(userId, otherUserId);
        return toResponses(userId, List.of(conversation)).get(0);
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPage<MessageResponse> listMessages(UUID userId, UUID conversationId, String before, Integer limit) {
        ConversationParticipant participant = requireParticipant(userId, conversationId);
        requireReachable(userId, conversationId);
        int size = limit == null ? DEFAULT_MESSAGE_PAGE : CursorPage.limit(limit);
        Cursor from = Cursor.decode(before);
        Instant visibleAfter = participant.getClearedAt() == null ? Instant.EPOCH : participant.getClearedAt();
        List<Message> messages =
                messageRepository.findPage(conversationId, visibleAfter, from.time(), from.id(), size + 1);
        return CursorPage.of(messages, size, message -> new Cursor(message.getCreatedAt(), message.getId()),
                messageMapper::toResponses);
    }

    @Override
    @Transactional
    public MessageResponse send(UUID userId, UUID conversationId, SendMessageRequest request) {
        if (request.clientMessageId() != null) {
            Optional<Message> existing =
                    messageRepository.findBySenderIdAndClientMessageId(userId, request.clientMessageId());
            if (existing.isPresent()) {
                return messageMapper.toResponse(existing.get());
            }
        }
        rateLimiter.check("message", userId, 30, Duration.ofMinutes(1));
        ConversationParticipant participant = requireParticipant(userId, conversationId);
        UUID recipientId = requireReachable(userId, conversationId);

        Message draft = buildMessage(userId, recipientId, conversationId, request);
        participant.setState(ParticipantState.INBOX);
        return messageMapper.toResponse(store(draft, recipientId));
    }

    @Override
    @Transactional
    public ReadReceiptResponse markRead(UUID userId, UUID conversationId, UUID lastReadMessageId) {
        ConversationParticipant participant = requireParticipant(userId, conversationId);
        UUID otherUserId = requireReachable(userId, conversationId);
        Instant readUpTo = lastReadMessageId == null
                ? Instant.now()
                : messageRepository.findById(lastReadMessageId)
                        .filter(message -> message.getConversationId().equals(conversationId))
                        .map(Message::getCreatedAt)
                        .orElseThrow(() -> ApiException.rule("The message does not belong to this conversation."));
        if (participant.getLastReadAt() == null || readUpTo.isAfter(participant.getLastReadAt())) {
            participant.setLastReadAt(readUpTo);
        }
        eventPublisher.publishEvent(
                new ConversationReadEvent(conversationId, userId, otherUserId, participant.getLastReadAt()));
        return new ReadReceiptResponse(conversationId, userId, participant.getLastReadAt());
    }

    @Override
    @Transactional
    public void accept(UUID userId, UUID conversationId) {
        requireParticipant(userId, conversationId).setState(ParticipantState.INBOX);
    }

    @Override
    @Transactional
    public void clear(UUID userId, UUID conversationId) {
        ConversationParticipant participant = requireParticipant(userId, conversationId);
        participant.setClearedAt(Instant.now());
        participant.setLastReadAt(Instant.now());
    }

    @Override
    @Transactional(readOnly = true)
    public UUID otherParticipant(UUID userId, UUID conversationId) {
        requireParticipant(userId, conversationId);
        return requireReachable(userId, conversationId);
    }

    @Override
    @Transactional
    public Message deliverDirect(UUID senderId, UUID recipientId, MessageType type, String text, UUID storyId) {
        Conversation conversation = findOrCreateRegular(senderId, recipientId);
        Message draft = Message.builder()
                .conversationId(conversation.getId())
                .senderId(senderId)
                .type(type)
                .text(text)
                .replyToStoryId(storyId)
                .build();
        return store(draft, recipientId);
    }

    @Override
    @Transactional
    public UUID openDating(UUID matchId, UUID firstUserId, UUID secondUserId) {
        Optional<Conversation> existing = conversationRepository.findByMatchId(matchId);
        if (existing.isPresent()) {
            return existing.get().getId();
        }
        Conversation conversation = conversationRepository.save(Conversation.builder()
                .type(ConversationType.DATING)
                .userAId(Pairs.first(firstUserId, secondUserId))
                .userBId(Pairs.second(firstUserId, secondUserId))
                .matchId(matchId)
                .build());
        saveParticipant(conversation.getId(), firstUserId, ParticipantState.INBOX);
        saveParticipant(conversation.getId(), secondUserId, ParticipantState.INBOX);
        return conversation.getId();
    }

    @Override
    @Transactional
    public void addIntro(UUID conversationId, UUID senderId, String text) {
        Message message = messageRepository.saveAndFlush(Message.builder()
                .conversationId(conversationId)
                .senderId(senderId)
                .type(MessageType.INTRO)
                .text(text)
                .build());
        conversationRepository.touch(conversationId, message.getCreatedAt());
    }

    private Message buildMessage(UUID userId, UUID recipientId, UUID conversationId, SendMessageRequest request) {
        MessageType type = parseType(request.type());
        Message.MessageBuilder message = Message.builder()
                .conversationId(conversationId)
                .senderId(userId)
                .type(type)
                .clientMessageId(request.clientMessageId())
                .text(Texts.clean(request.text(), MAX_TEXT, "text"));
        switch (type) {
            case TEXT -> Texts.required(request.text(), MAX_TEXT, "text");
            case IMAGE -> {
                requireField(request.mediaId(), "mediaId");
                mediaService.requireUsable(userId, request.mediaId(), MediaPurpose.CHAT);
                message.mediaId(request.mediaId());
            }
            case POST_SHARE -> {
                requireField(request.sharedPostId(), "sharedPostId");
                requireShareable(postRepository.findVisible(request.sharedPostId(), userId).isPresent()
                        && postRepository.findVisible(request.sharedPostId(), recipientId).isPresent());
                message.sharedPostId(request.sharedPostId());
            }
            case REEL_SHARE -> {
                requireField(request.sharedReelId(), "sharedReelId");
                requireShareable(reelRepository.findVisible(request.sharedReelId(), userId).isPresent()
                        && reelRepository.findVisible(request.sharedReelId(), recipientId).isPresent());
                message.sharedReelId(request.sharedReelId());
            }
            default -> throw ApiException.invalid("type is not allowed");
        }
        return message.build();
    }

    private MessageType parseType(String type) {
        if (type == null || type.isBlank()) {
            return MessageType.TEXT;
        }
        return switch (type.trim().toLowerCase()) {
            case "text" -> MessageType.TEXT;
            case "image" -> MessageType.IMAGE;
            case "shared_post", "post_share" -> MessageType.POST_SHARE;
            case "shared_reel", "reel_share" -> MessageType.REEL_SHARE;
            default -> throw ApiException.invalid("type must be one of: text, image, shared_post, shared_reel");
        };
    }

    private void requireField(UUID value, String field) {
        if (value == null) {
            throw ApiException.invalid(field + " is required for this message type");
        }
    }

    private void requireShareable(boolean visibleToBoth) {
        if (!visibleToBoth) {
            throw ApiException.rule("This cannot be shared with this person.");
        }
    }

    private Message store(Message draft, UUID recipientId) {
        Message message = messageRepository.saveAndFlush(draft);
        conversationRepository.touch(message.getConversationId(), message.getCreatedAt());
        boolean muted = participantRepository.findById(new ParticipantId(message.getConversationId(), recipientId))
                .map(ConversationParticipant::isMuted)
                .orElse(false);
        eventPublisher.publishEvent(new MessageSentEvent(message, recipientId, muted));
        return message;
    }

    private Conversation findOrCreateRegular(UUID userId, UUID otherUserId) {
        UUID userA = Pairs.first(userId, otherUserId);
        UUID userB = Pairs.second(userId, otherUserId);
        conversationRepository.lockPair("chat:" + userA + ":" + userB);
        Optional<Conversation> existing = conversationRepository.findRegular(userA, userB);
        if (existing.isPresent()) {
            return existing.get();
        }
        Conversation conversation = conversationRepository.save(Conversation.builder()
                .type(ConversationType.REGULAR)
                .userAId(userA)
                .userBId(userB)
                .build());
        boolean recipientFollowsSender = visibilityService.isAcceptedFollower(otherUserId, userId);
        saveParticipant(conversation.getId(), userId, ParticipantState.INBOX);
        saveParticipant(conversation.getId(), otherUserId,
                recipientFollowsSender ? ParticipantState.INBOX : ParticipantState.REQUEST);
        return conversation;
    }

    private void saveParticipant(UUID conversationId, UUID userId, ParticipantState state) {
        participantRepository.save(ConversationParticipant.builder()
                .id(new ParticipantId(conversationId, userId))
                .state(state)
                .build());
    }

    private ConversationParticipant requireParticipant(UUID userId, UUID conversationId) {
        return participantRepository.findById(new ParticipantId(conversationId, userId))
                .orElseThrow(ApiException::notFound);
    }

    private UUID requireReachable(UUID userId, UUID conversationId) {
        Conversation conversation = requireOpenConversation(conversationId);
        UUID otherUserId = Pairs.other(conversation.getUserAId(), conversation.getUserBId(), userId);
        if (visibilityService.isBlockedBetween(userId, otherUserId)) {
            throw ApiException.notFound();
        }
        return otherUserId;
    }

    private Conversation requireOpenConversation(UUID conversationId) {
        Conversation conversation = conversationRepository.findById(conversationId).orElseThrow(ApiException::notFound);
        if (conversation.getType() == ConversationType.DATING) {
            boolean active = matchRepository.findById(conversation.getMatchId())
                    .map(Match::getStatus)
                    .filter(status -> status == MatchStatus.ACTIVE)
                    .isPresent();
            if (!active) {
                throw new ApiException(ErrorCode.FORBIDDEN, "This match has ended.");
            }
        }
        return conversation;
    }

    private Instant sortTime(Conversation conversation) {
        return conversation.getLastMessageAt() != null ? conversation.getLastMessageAt() : conversation.getCreatedAt();
    }

    private List<ConversationResponse> toResponses(UUID userId, List<Conversation> conversations) {
        if (conversations.isEmpty()) {
            return List.of();
        }
        List<UUID> ids = conversations.stream().map(Conversation::getId).toList();
        Map<UUID, UserSummaryResponse> users = userMapper.summariesById(conversations.stream()
                .map(conversation -> Pairs.other(conversation.getUserAId(), conversation.getUserBId(), userId))
                .toList());
        Map<UUID, ConversationParticipant> participants = participantRepository.findForUser(userId, ids).stream()
                .collect(Collectors.toMap(participant -> participant.getId().getConversationId(), Function.identity()));
        Map<UUID, Long> unread = messageRepository.countUnread(userId, ids).stream()
                .collect(Collectors.toMap(UnreadRow::getConversationId, UnreadRow::getTotal));
        Map<UUID, MessageResponse> latest = messageMapper.toResponses(messageRepository.findLatest(ids)).stream()
                .collect(Collectors.toMap(MessageResponse::conversationId, Function.identity()));

        return conversations.stream()
                .map(conversation -> {
                    ConversationParticipant participant = participants.get(conversation.getId());
                    UUID otherId = Pairs.other(conversation.getUserAId(), conversation.getUserBId(), userId);
                    return new ConversationResponse(
                            conversation.getId(),
                            Enums.value(conversation.getType()),
                            users.get(otherId),
                            latest.get(conversation.getId()),
                            unread.getOrDefault(conversation.getId(), 0L),
                            participant.getState() == ParticipantState.REQUEST,
                            participant.isMuted(),
                            conversation.getMatchId(),
                            conversation.getLastMessageAt(),
                            conversation.getCreatedAt());
                })
                .toList();
    }
}
