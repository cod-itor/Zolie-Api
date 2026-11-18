package com.example.zolieapi.service;

import com.example.zolieapi.dto.request.SyncRequest;
import com.example.zolieapi.dto.request.UpdateProfileRequest;
import com.example.zolieapi.dto.response.AvailabilityResponse;
import com.example.zolieapi.dto.response.DeletionResponse;
import com.example.zolieapi.dto.response.MeResponse;
import com.example.zolieapi.dto.response.ResolveLoginResponse;
import com.example.zolieapi.security.AuthUser;
import java.util.UUID;

public interface AccountService {

    MeResponse sync(AuthUser authUser, SyncRequest request);

    MeResponse getMe(UUID userId);

    MeResponse updateProfile(UUID userId, UpdateProfileRequest request);

    void deleteAccount(UUID userId);

    DeletionResponse requestDeletion(UUID userId, String reason);

    void cancelDeletion(UUID userId);

    void revokeOtherSessions(UUID userId);

    AvailabilityResponse checkUsername(String username, String clientAddress);

    ResolveLoginResponse resolveLogin(String username, String clientAddress);
}
