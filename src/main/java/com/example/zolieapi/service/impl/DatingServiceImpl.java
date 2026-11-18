package com.example.zolieapi.service.impl;

import com.example.zolieapi.common.Ages;
import com.example.zolieapi.common.Cursor;
import com.example.zolieapi.common.CursorPage;
import com.example.zolieapi.common.Enums;
import com.example.zolieapi.common.Pairs;
import com.example.zolieapi.common.Texts;
import com.example.zolieapi.dto.request.DatingLocationRequest;
import com.example.zolieapi.dto.request.DatingPreferencesRequest;
import com.example.zolieapi.dto.request.DatingProfileRequest;
import com.example.zolieapi.dto.response.DatingCardResponse;
import com.example.zolieapi.dto.response.DatingLikeResponse;
import com.example.zolieapi.dto.response.DatingPreferencesResponse;
import com.example.zolieapi.dto.response.DatingProfileResponse;
import com.example.zolieapi.dto.response.DiscoveryResponse;
import com.example.zolieapi.dto.response.EligibilityResponse;
import com.example.zolieapi.dto.response.MatchResponse;
import com.example.zolieapi.dto.response.ReceivedLikeResponse;
import com.example.zolieapi.exception.ApiException;
import com.example.zolieapi.exception.ErrorCode;
import com.example.zolieapi.mapper.DatingMapper;
import com.example.zolieapi.model.Conversation;
import com.example.zolieapi.model.DatingIntro;
import com.example.zolieapi.model.DatingLike;
import com.example.zolieapi.model.DatingProfile;
import com.example.zolieapi.model.Match;
import com.example.zolieapi.model.User;
import com.example.zolieapi.model.enums.DatingLikeType;
import com.example.zolieapi.model.enums.DatingStatus;
import com.example.zolieapi.model.enums.Gender;
import com.example.zolieapi.model.enums.IntroStatus;
import com.example.zolieapi.model.enums.LookingFor;
import com.example.zolieapi.model.enums.MatchStatus;
import com.example.zolieapi.model.enums.MediaKind;
import com.example.zolieapi.model.enums.MediaPurpose;
import com.example.zolieapi.model.enums.NotificationTarget;
import com.example.zolieapi.model.enums.NotificationType;
import com.example.zolieapi.model.id.UserPairId;
import com.example.zolieapi.repository.ConversationRepository;
import com.example.zolieapi.repository.DatingIntroRepository;
import com.example.zolieapi.repository.DatingLikeRepository;
import com.example.zolieapi.repository.DatingProfileRepository;
import com.example.zolieapi.repository.DatingProfileRepository.CandidateRow;
import com.example.zolieapi.repository.DatingProfileRepository.PhotoRow;
import com.example.zolieapi.repository.InterestRepository;
import com.example.zolieapi.repository.MatchRepository;
import com.example.zolieapi.repository.UserRepository;
import com.example.zolieapi.security.RateLimiter;
import com.example.zolieapi.service.ConversationService;
import com.example.zolieapi.service.DatingService;
import com.example.zolieapi.service.MediaService;
import com.example.zolieapi.service.NotificationService;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DatingServiceImpl implements DatingService {

    private static final int MAX_PHOTOS = 6;
    private static final int MAX_DISCOVERY = 20;
    private static final int DEFAULT_DISCOVERY = 10;
    private static final int MIN_DISTANCE_KM = 1;
    private static final int MAX_DISTANCE_KM = 200;
    private static final int MAX_PREF_AGE = 99;
    private static final int MAX_INTRO = 500;
    private static final double LOCATION_PRECISION = 100.0;

    private final DatingProfileRepository profileRepository;
    private final DatingLikeRepository likeRepository;
    private final DatingIntroRepository introRepository;
    private final MatchRepository matchRepository;
    private final ConversationRepository conversationRepository;
    private final InterestRepository interestRepository;
    private final UserRepository userRepository;
    private final ConversationService conversationService;
    private final NotificationService notificationService;
    private final MediaService mediaService;
    private final RateLimiter rateLimiter;
    private final DatingMapper datingMapper;

    @Override
    @Transactional(readOnly = true)
    public EligibilityResponse eligibility(UUID userId) {
        if (!Ages.isAdult(requireUser(userId).getDateOfBirth())) {
            return new EligibilityResponse(false, "under_18");
        }
        Optional<DatingProfile> profile = profileRepository.findById(userId);
        if (profile.isEmpty()) {
            return new EligibilityResponse(false, "no_profile");
        }
        return switch (profile.get().getStatus()) {
            case ACTIVE -> new EligibilityResponse(true, null);
            case PAUSED -> new EligibilityResponse(false, "paused");
            case OFF -> new EligibilityResponse(false, "off");
        };
    }

    @Override
    @Transactional(readOnly = true)
    public DatingProfileResponse getProfile(UUID userId) {
        User user = requireAdult(userId);
        return datingMapper.toOwnProfile(requireProfile(userId), user);
    }

    @Override
    @Transactional
    public DatingProfileResponse saveProfile(UUID userId, DatingProfileRequest request) {
        User user = requireAdult(userId);
        DatingProfile profile = profileRepository.findById(userId)
                .orElseGet(() -> DatingProfile.builder().userId(userId).build());
        profile.setGender(Enums.require(Gender.class, request.gender(), "gender"));
        if (request.lookingFor() != null) {
            profile.setLookingFor(Enums.parse(LookingFor.class, request.lookingFor(), "lookingFor"));
        }
        profile.setBio(Texts.clean(request.bio()));
        profile.setEducation(Texts.clean(request.education()));
        profile.setOccupation(Texts.clean(request.occupation()));
        profile.setCity(Texts.clean(request.city()));
        applyVisibility(profile, request);
        profileRepository.saveAndFlush(profile);

        if (request.interestIds() != null) {
            replaceInterests(userId, request.interestIds());
        }
        return datingMapper.toOwnProfile(profile, user);
    }

    @Override
    @Transactional
    public DatingProfileResponse replacePhotos(UUID userId, List<UUID> mediaIds) {
        User user = requireAdult(userId);
        DatingProfile profile = requireProfile(userId);
        List<UUID> ids = mediaIds.stream().distinct().toList();
        if (ids.size() > MAX_PHOTOS) {
            throw ApiException.rule("A dating profile can have up to " + MAX_PHOTOS + " photos.");
        }
        ids.forEach(mediaId -> {
            if (mediaService.requireUsable(userId, mediaId, MediaPurpose.DATING).getKind() != MediaKind.IMAGE) {
                throw ApiException.rule("Dating photos must be images.");
            }
        });
        List<UUID> removed = profileRepository.findPhotos(List.of(userId)).stream()
                .map(PhotoRow::getMediaId)
                .filter(mediaId -> !ids.contains(mediaId))
                .toList();
        profileRepository.deletePhotos(userId);
        for (int position = 0; position < ids.size(); position++) {
            profileRepository.addPhoto(userId, ids.get(position), position);
        }
        mediaService.deleteAll(removed);
        return datingMapper.toOwnProfile(profile, user);
    }

    @Override
    @Transactional
    public DatingProfileResponse changeStatus(UUID userId, String status) {
        User user = requireAdult(userId);
        DatingProfile profile = requireProfile(userId);
        profile.setStatus(switch (status.trim().toLowerCase()) {
            case "on", "active" -> DatingStatus.ACTIVE;
            case "paused" -> DatingStatus.PAUSED;
            case "off" -> DatingStatus.OFF;
            default -> throw ApiException.invalid("status must be one of: on, paused, off");
        });
        return datingMapper.toOwnProfile(profileRepository.saveAndFlush(profile), user);
    }

    @Override
    @Transactional(readOnly = true)
    public DatingPreferencesResponse getPreferences(UUID userId) {
        requireAdult(userId);
        return toPreferences(requireProfile(userId));
    }

    @Override
    @Transactional
    public DatingPreferencesResponse savePreferences(UUID userId, DatingPreferencesRequest request) {
        requireAdult(userId);
        DatingProfile profile = requireProfile(userId);
        if (request.genders() != null) {
            if (request.genders().isEmpty()) {
                throw ApiException.rule("Pick at least one gender.");
            }
            profile.setPrefGenders(request.genders().stream()
                    .map(gender -> Enums.value(Enums.require(Gender.class, gender, "genders")))
                    .distinct()
                    .toArray(String[]::new));
        }
        int minAge = request.minAge() != null ? request.minAge() : profile.getPrefMinAge();
        int maxAge = request.maxAge() != null ? request.maxAge() : profile.getPrefMaxAge();
        if (minAge < Ages.ADULT_AGE || maxAge > MAX_PREF_AGE || minAge > maxAge) {
            throw ApiException.rule("The age range must be between 18 and 99.");
        }
        profile.setPrefMinAge(minAge);
        profile.setPrefMaxAge(maxAge);
        if (request.maxDistanceKm() != null) {
            if (request.maxDistanceKm() < MIN_DISTANCE_KM || request.maxDistanceKm() > MAX_DISTANCE_KM) {
                throw ApiException.rule("The distance must be between 1 and 200 km.");
            }
            profile.setMaxDistanceKm(request.maxDistanceKm());
        }
        return toPreferences(profile);
    }

    @Override
    @Transactional
    public void updateLocation(UUID userId, DatingLocationRequest request) {
        requireAdult(userId);
        requireProfile(userId);
        double lat = Math.round(request.lat() * LOCATION_PRECISION) / LOCATION_PRECISION;
        double lng = Math.round(request.lng() * LOCATION_PRECISION) / LOCATION_PRECISION;
        profileRepository.setLocation(userId, lat, lng);
    }

    @Override
    @Transactional(readOnly = true)
    public DiscoveryResponse discover(UUID userId, Integer limit) {
        requireActiveProfile(userId);
        if (!profileRepository.hasLocation(userId)) {
            return new DiscoveryResponse(List.of(), "location_required");
        }
        int size = limit == null ? DEFAULT_DISCOVERY : Math.max(1, Math.min(limit, MAX_DISCOVERY));
        List<CandidateRow> candidates = profileRepository.findCandidates(userId, size);
        Map<UUID, Double> distances = new HashMap<>();
        candidates.forEach(candidate -> distances.put(candidate.getUserId(), candidate.getDistanceMeters()));
        List<UUID> order = candidates.stream().map(CandidateRow::getUserId).toList();
        Map<UUID, DatingCardResponse> cards = datingMapper.toCards(order, distances);
        List<DatingCardResponse> items = order.stream().map(cards::get).filter(Objects::nonNull).toList();
        return new DiscoveryResponse(items, items.isEmpty() ? "no_matches_in_range" : null);
    }

    @Override
    @Transactional
    public DatingLikeResponse like(UUID userId, UUID toUserId) {
        rateLimiter.check("dating-like", userId, 100, Duration.ofHours(1));
        prepareAction(userId, toUserId);
        requireNotActedOn(userId, toUserId);
        likeRepository.saveAndFlush(DatingLike.builder().id(new UserPairId(userId, toUserId)).build());
        return matchIfMutual(userId, toUserId);
    }

    @Override
    @Transactional
    public void pass(UUID userId, UUID toUserId) {
        prepareAction(userId, toUserId);
        if (likeRepository.existsById(new UserPairId(userId, toUserId))) {
            throw new ApiException(ErrorCode.ALREADY_ACTED);
        }
        likeRepository.insertPass(userId, toUserId);
        introRepository.findById(new UserPairId(toUserId, userId))
                .filter(intro -> intro.getStatus() == IntroStatus.PENDING)
                .ifPresent(intro -> intro.setStatus(IntroStatus.PASSED));
    }

    @Override
    @Transactional
    public DatingLikeResponse sendIntro(UUID userId, UUID toUserId, String message) {
        rateLimiter.check("dating-intro", userId, 10, Duration.ofHours(1));
        prepareAction(userId, toUserId);
        UserPairId pair = new UserPairId(userId, toUserId);
        if (introRepository.existsById(pair)) {
            throw new ApiException(ErrorCode.INTRO_ALREADY_SENT);
        }
        if (likeRepository.existsPass(userId, toUserId) || isMatched(userId, toUserId)) {
            throw new ApiException(ErrorCode.ALREADY_ACTED);
        }
        DatingLike like = likeRepository.findById(pair).orElseGet(() -> DatingLike.builder().id(pair).build());
        like.setType(DatingLikeType.INTRO);
        likeRepository.saveAndFlush(like);
        introRepository.saveAndFlush(DatingIntro.builder()
                .id(pair)
                .messageText(Texts.required(message, MAX_INTRO, "message"))
                .build());
        notificationService.send(toUserId, userId, NotificationType.DATING_INTRO, NotificationTarget.USER, userId);
        return matchIfMutual(userId, toUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPage<ReceivedLikeResponse> listReceivedLikes(UUID userId, boolean introsOnly, String cursor,
                                                              Integer limit) {
        requireActiveProfile(userId);
        int size = CursorPage.limit(limit);
        Cursor from = Cursor.decode(cursor);
        List<DatingLike> likes = introsOnly
                ? likeRepository.findReceivedIntros(userId, from.time(), from.id(), size + 1)
                : likeRepository.findReceived(userId, from.time(), from.id(), size + 1);
        return CursorPage.of(likes, size, like -> new Cursor(like.getCreatedAt(), like.getId().getFromUserId()),
                this::toReceived);
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPage<MatchResponse> listMatches(UUID userId, String cursor, Integer limit) {
        requireAdult(userId);
        int size = CursorPage.limit(limit);
        Cursor from = Cursor.decode(cursor);
        List<Match> matches = matchRepository.findActiveForUser(userId, from.time(), from.id(), size + 1);
        return CursorPage.of(matches, size, match -> new Cursor(match.getCreatedAt(), match.getId()),
                page -> toMatches(userId, page));
    }

    @Override
    @Transactional
    public void unmatch(UUID userId, UUID matchId) {
        Match match = matchRepository.findById(matchId)
                .filter(found -> found.getUserAId().equals(userId) || found.getUserBId().equals(userId))
                .orElseThrow(ApiException::notFound);
        if (match.getStatus() != MatchStatus.ACTIVE) {
            return;
        }
        match.setStatus(MatchStatus.UNMATCHED);
        match.setEndedAt(Instant.now());
        match.setEndedBy(userId);
    }

    private void prepareAction(UUID userId, UUID toUserId) {
        if (userId.equals(toUserId)) {
            throw ApiException.rule("You cannot do this to yourself.");
        }
        requireActiveProfile(userId);
        if (!profileRepository.isDatable(toUserId, userId)) {
            throw ApiException.notFound();
        }
        profileRepository.lockPair(Pairs.first(userId, toUserId) + ":" + Pairs.second(userId, toUserId));
    }

    private void requireNotActedOn(UUID userId, UUID toUserId) {
        boolean acted = likeRepository.existsById(new UserPairId(userId, toUserId))
                || likeRepository.existsPass(userId, toUserId);
        if (acted) {
            throw new ApiException(ErrorCode.ALREADY_ACTED);
        }
    }

    private boolean isMatched(UUID userId, UUID otherUserId) {
        return matchRepository.findPair(userId, otherUserId).isPresent();
    }

    private DatingLikeResponse matchIfMutual(UUID userId, UUID otherUserId) {
        if (!likeRepository.existsById(new UserPairId(otherUserId, userId))) {
            return new DatingLikeResponse(false, null, null);
        }
        if (matchRepository.insertIfAbsent(userId, otherUserId) == 0) {
            return new DatingLikeResponse(false, null, null);
        }
        Match match = matchRepository.findPair(userId, otherUserId).orElseThrow();
        UUID conversationId = conversationService.openDating(match.getId(), userId, otherUserId);
        moveIntrosToChat(conversationId, userId, otherUserId);
        notificationService.send(otherUserId, userId, NotificationType.MATCH, NotificationTarget.MATCH, match.getId());
        notificationService.send(userId, otherUserId, NotificationType.MATCH, NotificationTarget.MATCH, match.getId());
        return new DatingLikeResponse(true, match.getId(), conversationId);
    }

    private void moveIntrosToChat(UUID conversationId, UUID userId, UUID otherUserId) {
        List<DatingIntro> intros = introRepository.findAllById(
                List.of(new UserPairId(otherUserId, userId), new UserPairId(userId, otherUserId)));
        intros.stream()
                .sorted((first, second) -> first.getSentAt().compareTo(second.getSentAt()))
                .forEach(intro -> {
                    conversationService.addIntro(conversationId, intro.getId().getFromUserId(), intro.getMessageText());
                    intro.setStatus(IntroStatus.MATCHED);
                });
    }

    private void applyVisibility(DatingProfile profile, DatingProfileRequest request) {
        if (request.showAge() != null) {
            profile.setShowAge(request.showAge());
        }
        if (request.showCity() != null) {
            profile.setShowCity(request.showCity());
        }
        if (request.showEducation() != null) {
            profile.setShowEducation(request.showEducation());
        }
        if (request.showOccupation() != null) {
            profile.setShowOccupation(request.showOccupation());
        }
        if (request.showDistance() != null) {
            profile.setShowDistance(request.showDistance());
        }
    }

    private void replaceInterests(UUID userId, List<Integer> interestIds) {
        List<Integer> ids = interestIds.stream().distinct().toList();
        if (interestRepository.findAllById(ids).size() != ids.size()) {
            throw ApiException.rule("One or more interests do not exist.");
        }
        interestRepository.deleteForDatingProfile(userId);
        ids.forEach(id -> interestRepository.addForDatingProfile(userId, id));
    }

    private User requireUser(UUID userId) {
        return userRepository.findById(userId).orElseThrow(ApiException::notFound);
    }

    private User requireAdult(UUID userId) {
        User user = requireUser(userId);
        if (!Ages.isAdult(user.getDateOfBirth())) {
            throw new ApiException(ErrorCode.UNDER_18);
        }
        return user;
    }

    private DatingProfile requireProfile(UUID userId) {
        return profileRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.DATING_PROFILE_REQUIRED));
    }

    private void requireActiveProfile(UUID userId) {
        requireAdult(userId);
        if (requireProfile(userId).getStatus() != DatingStatus.ACTIVE) {
            throw new ApiException(ErrorCode.DATING_PROFILE_REQUIRED, "Turn Dating on to continue.");
        }
    }

    private DatingPreferencesResponse toPreferences(DatingProfile profile) {
        return new DatingPreferencesResponse(Arrays.asList(profile.getPrefGenders()), profile.getPrefMinAge(),
                profile.getPrefMaxAge(), profile.getMaxDistanceKm());
    }

    private List<ReceivedLikeResponse> toReceived(List<DatingLike> likes) {
        List<UUID> senders = likes.stream().map(like -> like.getId().getFromUserId()).toList();
        Map<UUID, DatingCardResponse> cards = datingMapper.toCards(senders, Map.of());
        Map<UUID, String> intros = introRepository.findAllById(likes.stream().map(DatingLike::getId).toList()).stream()
                .collect(Collectors.toMap(intro -> intro.getId().getFromUserId(), DatingIntro::getMessageText));
        return likes.stream()
                .filter(like -> cards.containsKey(like.getId().getFromUserId()))
                .map(like -> new ReceivedLikeResponse(
                        cards.get(like.getId().getFromUserId()),
                        Enums.value(like.getType()),
                        intros.get(like.getId().getFromUserId()),
                        like.getCreatedAt()))
                .toList();
    }

    private List<MatchResponse> toMatches(UUID userId, List<Match> matches) {
        Map<UUID, UUID> otherByMatch = matches.stream().collect(Collectors.toMap(Match::getId,
                match -> Pairs.other(match.getUserAId(), match.getUserBId(), userId)));
        Map<UUID, DatingCardResponse> cards = datingMapper.toCards(otherByMatch.values(), Map.of());
        Map<UUID, Conversation> conversations = conversationRepository.findByMatchIdIn(otherByMatch.keySet()).stream()
                .collect(Collectors.toMap(Conversation::getMatchId, Function.identity()));
        return matches.stream()
                .map(match -> new MatchResponse(
                        match.getId(),
                        Optional.ofNullable(conversations.get(match.getId())).map(Conversation::getId).orElse(null),
                        cards.get(otherByMatch.get(match.getId())),
                        match.getCreatedAt()))
                .toList();
    }
}
