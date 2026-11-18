package com.example.zolieapi.service.impl;

import com.example.zolieapi.common.Cursor;
import com.example.zolieapi.common.CursorPage;
import com.example.zolieapi.common.Enums;
import com.example.zolieapi.dto.response.FollowResponse;
import com.example.zolieapi.dto.response.UserSummaryResponse;
import com.example.zolieapi.exception.ApiException;
import com.example.zolieapi.mapper.UserMapper;
import com.example.zolieapi.model.User;
import com.example.zolieapi.model.enums.AccountType;
import com.example.zolieapi.model.enums.FollowStatus;
import com.example.zolieapi.model.enums.NotificationTarget;
import com.example.zolieapi.model.enums.NotificationType;
import com.example.zolieapi.model.id.FollowId;
import com.example.zolieapi.repository.FollowRepository;
import com.example.zolieapi.repository.UserRepository;
import com.example.zolieapi.security.RateLimiter;
import com.example.zolieapi.service.FollowService;
import com.example.zolieapi.service.NotificationService;
import com.example.zolieapi.service.VisibilityService;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FollowServiceImpl implements FollowService {

    public static final String NONE = "none";
    public static final String REQUESTED = "requested";
    public static final String FOLLOWING = "following";

    private final FollowRepository followRepository;
    private final UserRepository userRepository;
    private final VisibilityService visibilityService;
    private final NotificationService notificationService;
    private final UserMapper userMapper;
    private final RateLimiter rateLimiter;

    @Override
    @Transactional
    public FollowResponse follow(UUID userId, UUID targetId) {
        if (userId.equals(targetId)) {
            throw ApiException.rule("You cannot follow yourself.");
        }
        rateLimiter.check("follow", userId, 60, Duration.ofMinutes(1));
        User target = visibilityService.requireReachableUser(userId, targetId);
        FollowStatus status = target.getAccountType() == AccountType.PRIVATE ? FollowStatus.PENDING : FollowStatus.ACCEPTED;

        boolean created = followRepository.insert(userId, targetId, Enums.value(status)) == 1;
        if (created && status == FollowStatus.ACCEPTED) {
            addCounts(userId, targetId, 1);
            notificationService.send(targetId, userId, NotificationType.FOLLOW, NotificationTarget.USER, userId);
        }
        if (created && status == FollowStatus.PENDING) {
            notificationService.send(targetId, userId, NotificationType.FOLLOW_REQUEST, NotificationTarget.USER, userId);
        }
        return new FollowResponse(statusOf(userId, targetId));
    }

    @Override
    @Transactional
    public FollowResponse unfollow(UUID userId, UUID targetId) {
        remove(userId, targetId);
        return new FollowResponse(NONE);
    }

    @Override
    @Transactional
    public void removeFollower(UUID userId, UUID followerId) {
        remove(followerId, userId);
    }

    @Override
    @Transactional
    public void removeBetween(UUID firstId, UUID secondId) {
        remove(firstId, secondId);
        remove(secondId, firstId);
    }

    @Override
    @Transactional(readOnly = true)
    public String statusOf(UUID followerId, UUID followedId) {
        return followRepository.findById(new FollowId(followerId, followedId))
                .map(follow -> follow.getStatus() == FollowStatus.ACCEPTED ? FOLLOWING : REQUESTED)
                .orElse(NONE);
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPage<UserSummaryResponse> listFollowers(UUID viewerId, UUID userId, String cursor, Integer limit) {
        visibilityService.requireCanSeeContentOf(viewerId, userId);
        int size = CursorPage.limit(limit);
        return page(userRepository.findFollowers(userId, viewerId, Cursor.decode(cursor).id(), size + 1), size);
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPage<UserSummaryResponse> listFollowing(UUID viewerId, UUID userId, String cursor, Integer limit) {
        visibilityService.requireCanSeeContentOf(viewerId, userId);
        int size = CursorPage.limit(limit);
        return page(userRepository.findFollowing(userId, viewerId, Cursor.decode(cursor).id(), size + 1), size);
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPage<UserSummaryResponse> listRequests(UUID userId, String cursor, Integer limit) {
        int size = CursorPage.limit(limit);
        return page(userRepository.findFollowRequesters(userId, Cursor.decode(cursor).id(), size + 1), size);
    }

    @Override
    @Transactional
    public void acceptRequest(UUID userId, UUID requesterId) {
        if (followRepository.acceptPending(requesterId, userId) == 0) {
            throw ApiException.notFound();
        }
        addCounts(requesterId, userId, 1);
        notificationService.send(requesterId, userId, NotificationType.FOLLOW_ACCEPTED, NotificationTarget.USER, userId);
    }

    @Override
    @Transactional
    public void declineRequest(UUID userId, UUID requesterId) {
        if (followRepository.deleteWithStatus(requesterId, userId, Enums.value(FollowStatus.PENDING)) == 0) {
            throw ApiException.notFound();
        }
    }

    @Override
    @Transactional
    public void acceptAllRequests(UUID userId) {
        followRepository.countPendingAsFollowing(userId);
        int accepted = followRepository.acceptAllPending(userId);
        if (accepted > 0) {
            userRepository.addFollowerCount(userId, accepted);
        }
    }

    private void remove(UUID followerId, UUID followedId) {
        if (followRepository.deleteWithStatus(followerId, followedId, Enums.value(FollowStatus.ACCEPTED)) == 1) {
            addCounts(followerId, followedId, -1);
        }
        followRepository.deleteWithStatus(followerId, followedId, Enums.value(FollowStatus.PENDING));
    }

    private void addCounts(UUID followerId, UUID followedId, int delta) {
        userRepository.addFollowingCount(followerId, delta);
        userRepository.addFollowerCount(followedId, delta);
    }

    private CursorPage<UserSummaryResponse> page(List<User> users, int size) {
        return CursorPage.of(users, size, user -> Cursor.ofId(user.getId()), userMapper::toSummaries);
    }
}
