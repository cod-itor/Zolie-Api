package com.example.zolieapi.service;

import com.example.zolieapi.dto.request.NotificationPreferencesRequest;
import com.example.zolieapi.dto.request.UpdateSettingsRequest;
import com.example.zolieapi.dto.response.NotificationPreferencesResponse;
import com.example.zolieapi.dto.response.SettingsResponse;
import java.util.UUID;

public interface SettingsService {

    SettingsResponse getSettings(UUID userId);

    SettingsResponse updateSettings(UUID userId, UpdateSettingsRequest request);

    NotificationPreferencesResponse getNotificationPreferences(UUID userId);

    NotificationPreferencesResponse updateNotificationPreferences(UUID userId, NotificationPreferencesRequest request);

    void registerDevice(UUID userId, String token);

    void removeDevice(UUID userId, String token);
}
