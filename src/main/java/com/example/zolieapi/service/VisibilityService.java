package com.example.zolieapi.service;

import com.example.zolieapi.model.User;
import com.example.zolieapi.model.enums.AudienceRule;
import java.util.UUID;

public interface VisibilityService {

    User requireActiveUser(UUID userId);

    User requireReachableUser(UUID viewerId, UUID userId);

    boolean isBlockedBetween(UUID firstId, UUID secondId);

    boolean canSeeContentOf(UUID viewerId, UUID authorId);

    void requireCanSeeContentOf(UUID viewerId, UUID authorId);

    boolean isAcceptedFollower(UUID followerId, UUID followedId);

    boolean allows(AudienceRule rule, UUID ownerId, UUID actorId);
}
