package com.example.zolieapi.service.impl;

import com.example.zolieapi.common.Cursor;
import com.example.zolieapi.common.CursorPage;
import com.example.zolieapi.dto.response.UserSummaryResponse;
import com.example.zolieapi.exception.ApiException;
import com.example.zolieapi.mapper.UserMapper;
import com.example.zolieapi.model.CloseFriend;
import com.example.zolieapi.model.id.CloseFriendId;
import com.example.zolieapi.repository.CloseFriendRepository;
import com.example.zolieapi.repository.UserRepository;
import com.example.zolieapi.service.CloseFriendService;
import com.example.zolieapi.service.VisibilityService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CloseFriendServiceImpl implements CloseFriendService {

    private final CloseFriendRepository closeFriendRepository;
    private final UserRepository userRepository;
    private final VisibilityService visibilityService;
    private final UserMapper userMapper;

    @Override
    @Transactional
    public void add(UUID userId, UUID friendId) {
        if (userId.equals(friendId)) {
            throw ApiException.rule("You cannot add yourself to Close Friends.");
        }
        visibilityService.requireReachableUser(userId, friendId);
        CloseFriendId id = new CloseFriendId(userId, friendId);
        if (!closeFriendRepository.existsById(id)) {
            closeFriendRepository.save(CloseFriend.builder().id(id).build());
        }
    }

    @Override
    @Transactional
    public void remove(UUID userId, UUID friendId) {
        CloseFriendId id = new CloseFriendId(userId, friendId);
        if (closeFriendRepository.existsById(id)) {
            closeFriendRepository.deleteById(id);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPage<UserSummaryResponse> list(UUID userId, String cursor, Integer limit) {
        int size = CursorPage.limit(limit);
        return CursorPage.of(userRepository.findCloseFriendsOf(userId, Cursor.decode(cursor).id(), size + 1), size,
                user -> Cursor.ofId(user.getId()), userMapper::toSummaries);
    }
}
