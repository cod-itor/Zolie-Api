package com.example.zolieapi.service;

import com.example.zolieapi.common.CursorPage;
import com.example.zolieapi.dto.request.DatingLocationRequest;
import com.example.zolieapi.dto.request.DatingPreferencesRequest;
import com.example.zolieapi.dto.request.DatingProfileRequest;
import com.example.zolieapi.dto.request.PromptAnswerRequest;
import com.example.zolieapi.dto.response.CountResponse;
import com.example.zolieapi.dto.response.DatingPromptResponse;
import com.example.zolieapi.dto.response.DatingLikeResponse;
import com.example.zolieapi.dto.response.DatingPreferencesResponse;
import com.example.zolieapi.dto.response.DatingProfileResponse;
import com.example.zolieapi.dto.response.DiscoveryResponse;
import com.example.zolieapi.dto.response.EligibilityResponse;
import com.example.zolieapi.dto.response.MatchResponse;
import com.example.zolieapi.dto.response.ReceivedLikeResponse;
import java.util.List;
import java.util.UUID;

public interface DatingService {

    EligibilityResponse eligibility(UUID userId);

    DatingProfileResponse getProfile(UUID userId);

    DatingProfileResponse saveProfile(UUID userId, DatingProfileRequest request);

    DatingProfileResponse replacePhotos(UUID userId, List<UUID> mediaIds);

    List<DatingPromptResponse> listPrompts();

    DatingProfileResponse replacePromptAnswers(UUID userId, List<PromptAnswerRequest> answers);

    CountResponse countReceivedThisWeek(UUID userId);

    DatingProfileResponse changeStatus(UUID userId, String status);

    DatingPreferencesResponse getPreferences(UUID userId);

    DatingPreferencesResponse savePreferences(UUID userId, DatingPreferencesRequest request);

    void updateLocation(UUID userId, DatingLocationRequest request);

    DiscoveryResponse discover(UUID userId, Integer limit);

    DatingLikeResponse like(UUID userId, UUID toUserId);

    void pass(UUID userId, UUID toUserId);

    DatingLikeResponse sendIntro(UUID userId, UUID toUserId, String message, UUID promptAnswerId);

    CursorPage<ReceivedLikeResponse> listReceivedLikes(UUID userId, boolean introsOnly, String cursor, Integer limit);

    CursorPage<MatchResponse> listMatches(UUID userId, String cursor, Integer limit);

    void unmatch(UUID userId, UUID matchId);
}
