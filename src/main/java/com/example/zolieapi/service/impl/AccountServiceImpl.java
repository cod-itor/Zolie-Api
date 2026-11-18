package com.example.zolieapi.service.impl;

import com.example.zolieapi.common.Ages;
import com.example.zolieapi.common.Enums;
import com.example.zolieapi.common.Texts;
import com.example.zolieapi.dto.request.SyncRequest;
import com.example.zolieapi.dto.request.UpdateProfileRequest;
import com.example.zolieapi.dto.response.AvailabilityResponse;
import com.example.zolieapi.dto.response.DatingSummaryResponse;
import com.example.zolieapi.dto.response.MeResponse;
import com.example.zolieapi.dto.response.ResolveLoginResponse;
import com.example.zolieapi.exception.ApiException;
import com.example.zolieapi.exception.ErrorCode;
import com.example.zolieapi.mapper.SettingsMapper;
import com.example.zolieapi.model.DatingProfile;
import com.example.zolieapi.model.NotificationPreference;
import com.example.zolieapi.model.User;
import com.example.zolieapi.dto.response.DeletionResponse;
import com.example.zolieapi.model.enums.AccountType;
import com.example.zolieapi.model.enums.MediaPurpose;
import com.example.zolieapi.repository.DatingProfileRepository;
import com.example.zolieapi.repository.NotificationPreferenceRepository;
import com.example.zolieapi.repository.UserRepository;
import com.example.zolieapi.security.AuthUser;
import com.example.zolieapi.security.RateLimiter;
import com.example.zolieapi.service.AccountService;
import com.example.zolieapi.service.MediaService;
import com.example.zolieapi.service.PlaceService;
import com.google.firebase.auth.AuthErrorCode;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private static final Pattern USERNAME = Pattern.compile("^[a-z0-9._]{3,30}$");
    private static final int MAX_DISPLAY_NAME = 50;
    private static final int MAX_BIO = 300;
    private static final int MAX_CITY = 80;
    private static final int MAX_WEBSITE = 200;
    private static final Duration DELETION_GRACE = Duration.ofDays(30);
    private static final Pattern WEBSITE = Pattern.compile("^(https?://)?[a-z0-9][a-z0-9.-]*\\.[a-z]{2,}(/[\\w\\-./?%&=#~+]*)?$", Pattern.CASE_INSENSITIVE);
    private static final String PHONE_EMAIL_DOMAIN = "@phone.zolie.app";
    private static final String UNKNOWN_EMAIL_DOMAIN = "@login.zolie.app";

    private final UserRepository userRepository;
    private final NotificationPreferenceRepository preferenceRepository;
    private final DatingProfileRepository datingProfileRepository;
    private final MediaService mediaService;
    private final PlaceService placeService;
    private final SettingsMapper settingsMapper;
    private final RateLimiter rateLimiter;
    private final ObjectProvider<FirebaseAuth> firebaseAuth;

    @Override
    @Transactional
    public MeResponse sync(AuthUser authUser, SyncRequest request) {
        Optional<User> existing = userRepository.findByFirebaseUid(authUser.firebaseUid());
        if (existing.isPresent()) {
            return toResponse(existing.get());
        }
        rateLimiter.check("sync", authUser.firebaseUid(), 10, Duration.ofHours(1));
        String username = normalizeUsername(request.username());
        String email = emailOf(authUser);
        validateNewAccount(request, username, email);

        User user = userRepository.saveAndFlush(User.builder()
                .firebaseUid(authUser.firebaseUid())
                .email(email)
                .username(username)
                .displayName(Texts.required(request.displayName(), MAX_DISPLAY_NAME, "displayName"))
                .dateOfBirth(request.dateOfBirth())
                .accountType(Ages.isAdult(request.dateOfBirth()) ? AccountType.PUBLIC : AccountType.PRIVATE)
                .termsAcceptedAt(Instant.now())
                .build());
        preferenceRepository.save(NotificationPreference.builder().userId(user.getId()).build());
        return toResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public MeResponse getMe(UUID userId) {
        return toResponse(requireUser(userId));
    }

    @Override
    @Transactional
    public MeResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        if (request.dateOfBirth() != null) {
            throw ApiException.rule("The date of birth cannot be changed after sign-up.");
        }
        User user = requireUser(userId);
        if (request.username() != null) {
            changeUsername(user, normalizeUsername(request.username()));
        }
        if (request.displayName() != null) {
            user.setDisplayName(Texts.required(request.displayName(), MAX_DISPLAY_NAME, "displayName"));
        }
        if (request.bio() != null) {
            user.setBio(Texts.clean(request.bio(), MAX_BIO, "bio"));
        }
        if (request.khmerName() != null) {
            user.setKhmerName(Texts.clean(request.khmerName(), MAX_DISPLAY_NAME, "khmerName"));
        }
        if (request.websiteUrl() != null) {
            user.setWebsiteUrl(cleanWebsite(request.websiteUrl()));
        }
        if (request.city() != null) {
            user.setCity(Texts.clean(request.city(), MAX_CITY, "city"));
        }
        if (request.coverMediaId() != null) {
            mediaService.requireUsable(userId, request.coverMediaId(), MediaPurpose.PROFILE);
            user.setCoverPhotoId(request.coverMediaId());
        }
        if (request.favoritePlaceIds() != null) {
            placeService.replaceFavorites(userId, request.favoritePlaceIds());
        }
        if (request.avatarMediaId() != null) {
            mediaService.requireUsable(userId, request.avatarMediaId(), MediaPurpose.PROFILE);
            user.setProfilePhotoId(request.avatarMediaId());
        }
        return toResponse(user);
    }

    @Override
    @Transactional
    public void deleteAccount(UUID userId) {
        User user = requireUser(userId);
        userRepository.releaseFollowerCounts(userId);
        userRepository.releaseFollowingCounts(userId);
        userRepository.releasePostLikeCounts(userId);
        userRepository.releaseReelLikeCounts(userId);
        userRepository.releasePollVoteCounts(userId);
        userRepository.releasePostCommentCounts(userId);
        userRepository.releaseReelCommentCounts(userId);
        userRepository.releaseCommentLikeCounts(userId);
        userRepository.releaseEventGoingCounts(userId);
        userRepository.releaseEventInterestedCounts(userId);
        userRepository.delete(user);
        userRepository.flush();
        deleteFirebaseAccount(user.getFirebaseUid());
    }

    @Override
    @Transactional
    public DeletionResponse requestDeletion(UUID userId, String reason) {
        User user = requireUser(userId);
        Instant now = Instant.now();
        user.setDeletionRequestedAt(now);
        user.setDeleteAfter(now.plus(DELETION_GRACE));
        user.setDeletionReason(Texts.clean(reason, MAX_WEBSITE, "reason"));
        return new DeletionResponse(user.getDeleteAfter());
    }

    @Override
    @Transactional
    public void cancelDeletion(UUID userId) {
        User user = requireUser(userId);
        user.setDeletionRequestedAt(null);
        user.setDeleteAfter(null);
        user.setDeletionReason(null);
    }

    @Override
    @Transactional
    public void revokeOtherSessions(UUID userId) {
        User user = requireUser(userId);
        FirebaseAuth auth = firebaseAuth.getIfAvailable();
        if (auth == null) {
            return;
        }
        try {
            auth.revokeRefreshTokens(user.getFirebaseUid());
        } catch (FirebaseAuthException e) {
            log.error("Could not revoke sessions: code={}", e.getAuthErrorCode());
            throw new ApiException(ErrorCode.INTERNAL_ERROR);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public AvailabilityResponse checkUsername(String username, String clientAddress) {
        rateLimiter.check("username-check", clientAddress, 30, Duration.ofMinutes(1));
        String normalized = username.trim().toLowerCase();
        boolean available = USERNAME.matcher(normalized).matches() && !userRepository.existsByUsername(normalized);
        return new AvailabilityResponse(normalized, available);
    }

    @Override
    @Transactional(readOnly = true)
    public ResolveLoginResponse resolveLogin(String username, String clientAddress) {
        rateLimiter.check("resolve-login", clientAddress, 10, Duration.ofMinutes(1));
        String normalized = username.trim().toLowerCase();
        String email = userRepository.findByUsername(normalized)
                .map(User::getEmail)
                .orElse(normalized.replaceAll("[^a-z0-9._]", "") + UNKNOWN_EMAIL_DOMAIN);
        return new ResolveLoginResponse(email);
    }

    private String cleanWebsite(String websiteUrl) {
        String cleaned = Texts.clean(websiteUrl, MAX_WEBSITE, "websiteUrl");
        if (cleaned != null && !WEBSITE.matcher(cleaned).matches()) {
            throw ApiException.rule("websiteUrl is not a valid link.");
        }
        return cleaned;
    }

    private void validateNewAccount(SyncRequest request, String username, String email) {
        if (!request.termsAccepted()) {
            throw ApiException.rule("You must accept the terms to create an account.");
        }
        LocalDate dateOfBirth = request.dateOfBirth();
        if (dateOfBirth.isAfter(LocalDate.now()) || dateOfBirth.isBefore(LocalDate.of(1900, 1, 2))) {
            throw ApiException.invalid("dateOfBirth is not valid");
        }
        if (Ages.of(dateOfBirth) < Ages.MINIMUM_AGE) {
            throw new ApiException(ErrorCode.UNDER_13);
        }
        if (userRepository.existsByUsername(username)) {
            throw new ApiException(ErrorCode.USERNAME_TAKEN);
        }
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ApiException(ErrorCode.EMAIL_TAKEN);
        }
    }

    private void changeUsername(User user, String username) {
        if (username.equals(user.getUsername())) {
            return;
        }
        if (userRepository.existsByUsername(username)) {
            throw new ApiException(ErrorCode.USERNAME_TAKEN);
        }
        user.setUsername(username);
    }

    private String normalizeUsername(String username) {
        String normalized = username.trim().toLowerCase();
        if (!USERNAME.matcher(normalized).matches()) {
            throw ApiException.rule("username must be 3 to 30 characters: letters, numbers, dots or underscores.");
        }
        return normalized;
    }

    private String emailOf(AuthUser authUser) {
        if (authUser.email() != null && !authUser.email().isBlank()) {
            return authUser.email().toLowerCase();
        }
        return authUser.firebaseUid().toLowerCase() + PHONE_EMAIL_DOMAIN;
    }

    private void deleteFirebaseAccount(String firebaseUid) {
        FirebaseAuth auth = firebaseAuth.getIfAvailable();
        if (auth == null) {
            return;
        }
        try {
            auth.deleteUser(firebaseUid);
        } catch (FirebaseAuthException e) {
            if (e.getAuthErrorCode() != AuthErrorCode.USER_NOT_FOUND) {
                log.error("Could not delete the Firebase account: code={}", e.getAuthErrorCode());
                throw new ApiException(ErrorCode.INTERNAL_ERROR);
            }
        }
    }

    private User requireUser(UUID userId) {
        return userRepository.findById(userId).orElseThrow(ApiException::notFound);
    }

    private MeResponse toResponse(User user) {
        String datingStatus = datingProfileRepository.findById(user.getId())
                .map(DatingProfile::getStatus)
                .map(Enums::value)
                .orElse(null);
        DatingSummaryResponse dating = new DatingSummaryResponse(Ages.isAdult(user.getDateOfBirth()), datingStatus);
        return new MeResponse(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getKhmerName(),
                user.getBio(),
                user.getWebsiteUrl(),
                user.getCity(),
                mediaService.url(user.getProfilePhotoId()),
                mediaService.url(user.getCoverPhotoId()),
                user.getEmail(),
                Enums.value(user.getAccountType()),
                Enums.value(user.getRole()),
                Enums.value(user.getStatus()),
                Ages.group(user.getDateOfBirth()),
                user.getFollowerCount(),
                user.getFollowingCount(),
                user.getPostCount(),
                placeService.favoritesOf(user.getId()),
                settingsMapper.toResponse(user),
                dating,
                user.getDeleteAfter(),
                user.getCreatedAt());
    }
}
