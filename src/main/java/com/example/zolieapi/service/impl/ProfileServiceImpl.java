package com.example.zolieapi.service.impl;

import com.example.zolieapi.dto.response.ProfileResponse;
import com.example.zolieapi.exception.ApiException;
import com.example.zolieapi.mapper.UserMapper;
import com.example.zolieapi.model.User;
import com.example.zolieapi.model.enums.AccountStatus;
import com.example.zolieapi.model.enums.AccountType;
import com.example.zolieapi.model.id.BlockId;
import com.example.zolieapi.repository.BlockRepository;
import com.example.zolieapi.repository.InsightRepository;
import com.example.zolieapi.repository.UserRepository;
import com.example.zolieapi.security.ActivityTracker;
import com.example.zolieapi.security.Viewer;
import com.example.zolieapi.service.FollowService;
import com.example.zolieapi.service.MediaService;
import com.example.zolieapi.service.PlaceService;
import com.example.zolieapi.service.ProfileService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService {

    private static final int MUTUAL_PREVIEW = 2;

    private final UserRepository userRepository;
    private final BlockRepository blockRepository;
    private final FollowService followService;
    private final MediaService mediaService;
    private final PlaceService placeService;
    private final InsightRepository insightRepository;
    private final UserMapper userMapper;

    @Override
    @Transactional
    public ProfileResponse getByUsername(UUID viewerId, String username) {
        User user = userRepository.findByUsername(username.trim().toLowerCase())
                .filter(found -> found.getStatus() == AccountStatus.ACTIVE && found.getDeletedAt() == null)
                .orElseThrow(ApiException::notFound);
        boolean isPrivate = user.getAccountType() == AccountType.PRIVATE;
        if (blockRepository.existsById(new BlockId(user.getId(), viewerId)) || (Viewer.isGuest(viewerId) && isPrivate)) {
            throw ApiException.notFound();
        }

        boolean isMe = user.getId().equals(viewerId);
        boolean blockedByMe = blockRepository.existsById(new BlockId(viewerId, user.getId()));
        String followStatus = followService.statusOf(viewerId, user.getId());
        boolean followsMe = FollowServiceImpl.FOLLOWING.equals(followService.statusOf(user.getId(), viewerId));
        boolean canView = !blockedByMe && (isMe || !isPrivate || FollowServiceImpl.FOLLOWING.equals(followStatus));

        if (!isMe && !Viewer.isGuest(viewerId) && !blockedByMe) {
            insightRepository.recordProfileVisit(user.getId(), viewerId);
        }
        boolean mutualsVisible = !isMe && !Viewer.isGuest(viewerId);
        List<User> mutuals = mutualsVisible
                ? userRepository.findMutualFollowers(viewerId, user.getId(), MUTUAL_PREVIEW)
                : List.of();
        long mutualCount = mutualsVisible ? userRepository.countMutualFollowers(viewerId, user.getId()) : 0;

        return new ProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getKhmerName(),
                user.getBio(),
                canView ? user.getWebsiteUrl() : null,
                canView ? user.getCity() : null,
                mediaService.url(user.getProfilePhotoId()),
                mediaService.url(user.getCoverPhotoId()),
                isPrivate,
                isMe,
                canView,
                followStatus,
                followsMe,
                blockedByMe,
                canView && isOnline(viewerId, user),
                user.getFollowerCount(),
                user.getFollowingCount(),
                user.getPostCount(),
                canView ? placeService.favoritesOf(user.getId()) : List.of(),
                userMapper.toSummaries(mutuals),
                mutualCount,
                user.getCreatedAt());
    }

    private boolean isOnline(UUID viewerId, User user) {
        if (!user.isShowOnlineStatus() || !ActivityTracker.isOnline(user.getLastActiveAt())) {
            return false;
        }
        return userRepository.findById(viewerId).map(User::isShowOnlineStatus).orElse(false);
    }
}
