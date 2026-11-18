package com.example.zolieapi.controller;

import com.example.zolieapi.dto.request.DeviceRequest;
import com.example.zolieapi.dto.request.NotificationPreferencesRequest;
import com.example.zolieapi.dto.request.UpdateSettingsRequest;
import com.example.zolieapi.dto.response.DeviceResponse;
import com.example.zolieapi.dto.response.NotificationPreferencesResponse;
import com.example.zolieapi.dto.response.SettingsResponse;
import com.example.zolieapi.security.CurrentUser;
import com.example.zolieapi.service.SettingsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/me")
@RequiredArgsConstructor
@Tag(name = "02. Settings")
public class SettingsController {

    private final SettingsService settingsService;
    private final CurrentUser currentUser;

    @GetMapping("/settings")
    @Operation(summary = "Get my privacy, theme and language settings")
    public SettingsResponse getSettings() {
        return settingsService.getSettings(currentUser.id());
    }

    @PatchMapping("/settings")
    @Operation(summary = "Change private account, online status, theme or language")
    public SettingsResponse updateSettings(@Valid @RequestBody UpdateSettingsRequest request) {
        return settingsService.updateSettings(currentUser.id(), request);
    }

    @GetMapping("/notification-preferences")
    @Operation(summary = "Get my notification switches")
    public NotificationPreferencesResponse getNotificationPreferences() {
        return settingsService.getNotificationPreferences(currentUser.id());
    }

    @PutMapping("/notification-preferences")
    @Operation(summary = "Turn notification types on or off")
    public NotificationPreferencesResponse updateNotificationPreferences(
            @Valid @RequestBody NotificationPreferencesRequest request) {
        return settingsService.updateNotificationPreferences(currentUser.id(), request);
    }

    @PostMapping("/devices")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Save or refresh this phone's push token")
    public void registerDevice(@Valid @RequestBody DeviceRequest request) {
        settingsService.registerDevice(currentUser.id(), request.token(), request.deviceName());
    }

    @GetMapping("/devices")
    @Operation(summary = "List the phones I am logged in on")
    public List<DeviceResponse> listDevices() {
        return settingsService.listDevices(currentUser.id());
    }

    @DeleteMapping("/devices/{token}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove this phone's push token on log out")
    public void removeDevice(@PathVariable String token) {
        settingsService.removeDevice(currentUser.idAllowingSuspended(), token);
    }
}
