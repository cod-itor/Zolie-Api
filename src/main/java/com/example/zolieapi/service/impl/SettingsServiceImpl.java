package com.example.zolieapi.service.impl;

import com.example.zolieapi.dto.request.NotificationPreferencesRequest;
import com.example.zolieapi.dto.request.UpdateSettingsRequest;
import com.example.zolieapi.dto.response.NotificationPreferencesResponse;
import com.example.zolieapi.dto.response.SettingsResponse;
import com.example.zolieapi.exception.ApiException;
import com.example.zolieapi.model.DeviceToken;
import com.example.zolieapi.model.NotificationPreference;
import com.example.zolieapi.model.User;
import com.example.zolieapi.model.enums.AccountType;
import com.example.zolieapi.repository.DeviceTokenRepository;
import com.example.zolieapi.repository.NotificationPreferenceRepository;
import com.example.zolieapi.repository.UserRepository;
import com.example.zolieapi.service.FollowService;
import com.example.zolieapi.service.SettingsService;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SettingsServiceImpl implements SettingsService {

    private static final Set<String> THEMES = Set.of("system", "light", "dark");
    private static final Set<String> LOCALES = Set.of("km", "en");

    private final UserRepository userRepository;
    private final NotificationPreferenceRepository preferenceRepository;
    private final DeviceTokenRepository deviceTokenRepository;
    private final FollowService followService;

    @Override
    @Transactional(readOnly = true)
    public SettingsResponse getSettings(UUID userId) {
        return toResponse(requireUser(userId));
    }

    @Override
    @Transactional
    public SettingsResponse updateSettings(UUID userId, UpdateSettingsRequest request) {
        User user = requireUser(userId);
        if (request.theme() != null) {
            user.setTheme(requireOneOf(request.theme(), THEMES, "theme"));
        }
        if (request.locale() != null) {
            user.setLocale(requireOneOf(request.locale(), LOCALES, "locale"));
        }
        if (request.showOnlineStatus() != null) {
            user.setShowOnlineStatus(request.showOnlineStatus());
        }
        if (request.isPrivate() != null) {
            changePrivacy(user, request.isPrivate());
        }
        return toResponse(user);
    }

    @Override
    @Transactional
    public NotificationPreferencesResponse getNotificationPreferences(UUID userId) {
        return toResponse(requirePreference(userId));
    }

    @Override
    @Transactional
    public NotificationPreferencesResponse updateNotificationPreferences(UUID userId,
                                                                         NotificationPreferencesRequest request) {
        NotificationPreference preference = requirePreference(userId);
        if (request.follows() != null) {
            preference.setFollows(request.follows());
        }
        if (request.likes() != null) {
            preference.setLikes(request.likes());
        }
        if (request.comments() != null) {
            preference.setComments(request.comments());
        }
        if (request.mentions() != null) {
            preference.setMentions(request.mentions());
        }
        if (request.matches() != null) {
            preference.setMatches(request.matches());
        }
        if (request.messages() != null) {
            preference.setMessages(request.messages());
        }
        if (request.storyInteractions() != null) {
            preference.setStoryInteractions(request.storyInteractions());
        }
        return toResponse(preference);
    }

    @Override
    @Transactional
    public void registerDevice(UUID userId, String token) {
        DeviceToken device = deviceTokenRepository.findByToken(token)
                .orElseGet(() -> DeviceToken.builder().token(token).build());
        device.setUserId(userId);
        deviceTokenRepository.save(device);
    }

    @Override
    @Transactional
    public void removeDevice(UUID userId, String token) {
        deviceTokenRepository.deleteByUserIdAndToken(userId, token);
    }

    private void changePrivacy(User user, boolean makePrivate) {
        boolean wasPrivate = user.getAccountType() == AccountType.PRIVATE;
        user.setAccountType(makePrivate ? AccountType.PRIVATE : AccountType.PUBLIC);
        userRepository.saveAndFlush(user);
        if (wasPrivate && !makePrivate) {
            followService.acceptAllRequests(user.getId());
        }
    }

    private String requireOneOf(String value, Set<String> allowed, String field) {
        String normalized = value.trim().toLowerCase();
        if (!allowed.contains(normalized)) {
            throw ApiException.invalid(field + " must be one of: " + String.join(", ", allowed.stream().sorted().toList()));
        }
        return normalized;
    }

    private User requireUser(UUID userId) {
        return userRepository.findById(userId).orElseThrow(ApiException::notFound);
    }

    private NotificationPreference requirePreference(UUID userId) {
        return preferenceRepository.findById(userId)
                .orElseGet(() -> preferenceRepository.save(NotificationPreference.builder().userId(userId).build()));
    }

    private SettingsResponse toResponse(User user) {
        return new SettingsResponse(user.getAccountType() == AccountType.PRIVATE, user.isShowOnlineStatus(),
                user.getTheme(), user.getLocale());
    }

    private NotificationPreferencesResponse toResponse(NotificationPreference preference) {
        return new NotificationPreferencesResponse(preference.isFollows(), preference.isLikes(),
                preference.isComments(), preference.isMentions(), preference.isMatches(), preference.isMessages(),
                preference.isStoryInteractions());
    }
}
