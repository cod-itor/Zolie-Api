package com.example.zolieapi.service.impl;

import com.example.zolieapi.dto.request.NotificationPreferencesRequest;
import com.example.zolieapi.dto.request.UpdateSettingsRequest;
import com.example.zolieapi.dto.response.NotificationPreferencesResponse;
import com.example.zolieapi.dto.response.SettingsResponse;
import com.example.zolieapi.common.Enums;
import com.example.zolieapi.common.Texts;
import com.example.zolieapi.dto.response.DeviceResponse;
import com.example.zolieapi.exception.ApiException;
import com.example.zolieapi.mapper.SettingsMapper;
import com.example.zolieapi.model.enums.AudienceRule;
import com.example.zolieapi.model.DeviceToken;
import com.example.zolieapi.model.NotificationPreference;
import com.example.zolieapi.model.User;
import com.example.zolieapi.model.enums.AccountType;
import com.example.zolieapi.repository.DeviceTokenRepository;
import com.example.zolieapi.repository.NotificationPreferenceRepository;
import com.example.zolieapi.repository.UserRepository;
import com.example.zolieapi.service.FollowService;
import com.example.zolieapi.service.SettingsService;
import java.time.Instant;
import java.util.List;
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
    private final SettingsMapper settingsMapper;

    @Override
    @Transactional(readOnly = true)
    public SettingsResponse getSettings(UUID userId) {
        return settingsMapper.toResponse(requireUser(userId));
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
        if (request.readReceipts() != null) {
            user.setReadReceipts(request.readReceipts());
        }
        if (request.dataSaver() != null) {
            user.setDataSaver(request.dataSaver());
        }
        if (request.hideDatingFromFollowing() != null) {
            user.setHideDatingFromFollowing(request.hideDatingFromFollowing());
        }
        if (request.whoCanMessage() != null) {
            user.setWhoCanMessage(Enums.parse(AudienceRule.class, request.whoCanMessage(), "whoCanMessage"));
        }
        if (request.whoCanMention() != null) {
            user.setWhoCanMention(Enums.parse(AudienceRule.class, request.whoCanMention(), "whoCanMention"));
        }
        if (request.storyRepliesFrom() != null) {
            user.setStoryRepliesFrom(Enums.parse(AudienceRule.class, request.storyRepliesFrom(), "storyRepliesFrom"));
        }
        if (request.isPrivate() != null) {
            changePrivacy(user, request.isPrivate());
        }
        return settingsMapper.toResponse(user);
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
        if (request.events() != null) {
            preference.setEvents(request.events());
        }
        if (request.pausedUntil() != null) {
            preference.setPausedUntil(request.pausedUntil().isAfter(Instant.now()) ? request.pausedUntil() : null);
        }
        return toResponse(preference);
    }

    @Override
    @Transactional
    public void registerDevice(UUID userId, String token, String deviceName) {
        DeviceToken device = deviceTokenRepository.findByToken(token)
                .orElseGet(() -> DeviceToken.builder().token(token).build());
        device.setUserId(userId);
        device.setLastSeenAt(Instant.now());
        if (deviceName != null) {
            device.setDeviceName(Texts.clean(deviceName));
        }
        deviceTokenRepository.save(device);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeviceResponse> listDevices(UUID userId) {
        return deviceTokenRepository.findByUserId(userId).stream()
                .map(device -> new DeviceResponse(device.getId(), device.getDeviceName(), device.getPlatform(),
                        device.getLastSeenAt()))
                .toList();
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

    private NotificationPreferencesResponse toResponse(NotificationPreference preference) {
        return new NotificationPreferencesResponse(preference.isFollows(), preference.isLikes(),
                preference.isComments(), preference.isMentions(), preference.isMatches(), preference.isMessages(),
                preference.isStoryInteractions(), preference.isEvents(), preference.getPausedUntil());
    }
}
