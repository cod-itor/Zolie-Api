package com.example.zolieapi.service;

import com.example.zolieapi.common.CursorPage;
import com.example.zolieapi.dto.response.FollowResponse;
import com.example.zolieapi.dto.response.UserSummaryResponse;
import java.util.UUID;

public interface FollowService {

    FollowResponse follow(UUID userId, UUID targetId);

    FollowResponse unfollow(UUID userId, UUID targetId);

    void removeFollower(UUID userId, UUID followerId);

    void removeBetween(UUID firstId, UUID secondId);

    String statusOf(UUID followerId, UUID followedId);

    CursorPage<UserSummaryResponse> listFollowers(UUID viewerId, UUID userId, String cursor, Integer limit);

    CursorPage<UserSummaryResponse> listFollowing(UUID viewerId, UUID userId, String cursor, Integer limit);

    CursorPage<UserSummaryResponse> listRequests(UUID userId, String cursor, Integer limit);

    void acceptRequest(UUID userId, UUID requesterId);

    void declineRequest(UUID userId, UUID requesterId);

    void acceptAllRequests(UUID userId);
}
