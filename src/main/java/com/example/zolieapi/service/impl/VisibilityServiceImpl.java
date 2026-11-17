package com.example.zolieapi.service.impl;

import com.example.zolieapi.exception.ApiException;
import com.example.zolieapi.model.User;
import com.example.zolieapi.model.enums.AccountStatus;
import com.example.zolieapi.model.enums.FollowStatus;
import com.example.zolieapi.model.id.FollowId;
import com.example.zolieapi.repository.BlockRepository;
import com.example.zolieapi.repository.FollowRepository;
import com.example.zolieapi.repository.UserRepository;
import com.example.zolieapi.service.VisibilityService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VisibilityServiceImpl implements VisibilityService {

    private final UserRepository userRepository;
    private final BlockRepository blockRepository;
    private final FollowRepository followRepository;

    @Override
    public User requireActiveUser(UUID userId) {
        return userRepository.findById(userId)
                .filter(user -> user.getStatus() == AccountStatus.ACTIVE && user.getDeletedAt() == null)
                .orElseThrow(ApiException::notFound);
    }

    @Override
    public User requireReachableUser(UUID viewerId, UUID userId) {
        User user = requireActiveUser(userId);
        if (isBlockedBetween(viewerId, userId)) {
            throw ApiException.notFound();
        }
        return user;
    }

    @Override
    public boolean isBlockedBetween(UUID firstId, UUID secondId) {
        return blockRepository.existsBetween(firstId, secondId);
    }

    @Override
    public boolean canSeeContentOf(UUID viewerId, UUID authorId) {
        return userRepository.canSeeContent(viewerId, authorId);
    }

    @Override
    public void requireCanSeeContentOf(UUID viewerId, UUID authorId) {
        if (!canSeeContentOf(viewerId, authorId)) {
            throw ApiException.notFound();
        }
    }

    @Override
    public boolean isAcceptedFollower(UUID followerId, UUID followedId) {
        return followRepository.findById(new FollowId(followerId, followedId))
                .filter(follow -> follow.getStatus() == FollowStatus.ACCEPTED)
                .isPresent();
    }
}
