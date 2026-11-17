package com.example.zolieapi.service.impl;

import com.example.zolieapi.common.Cursor;
import com.example.zolieapi.common.CursorPage;
import com.example.zolieapi.dto.response.UserSummaryResponse;
import com.example.zolieapi.exception.ApiException;
import com.example.zolieapi.mapper.UserMapper;
import com.example.zolieapi.model.Block;
import com.example.zolieapi.model.id.BlockId;
import com.example.zolieapi.repository.BlockRepository;
import com.example.zolieapi.repository.CloseFriendRepository;
import com.example.zolieapi.repository.MatchRepository;
import com.example.zolieapi.repository.UserRepository;
import com.example.zolieapi.service.BlockService;
import com.example.zolieapi.service.FollowService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BlockServiceImpl implements BlockService {

    private final BlockRepository blockRepository;
    private final CloseFriendRepository closeFriendRepository;
    private final MatchRepository matchRepository;
    private final UserRepository userRepository;
    private final FollowService followService;
    private final UserMapper userMapper;

    @Override
    @Transactional
    public void block(UUID userId, UUID targetId) {
        if (userId.equals(targetId)) {
            throw ApiException.rule("You cannot block yourself.");
        }
        if (!userRepository.existsById(targetId)) {
            throw ApiException.notFound();
        }
        BlockId id = new BlockId(userId, targetId);
        if (!blockRepository.existsById(id)) {
            blockRepository.save(Block.builder().id(id).build());
        }
        followService.removeBetween(userId, targetId);
        closeFriendRepository.deleteBetween(userId, targetId);
        matchRepository.blockBetween(userId, targetId, userId);
    }

    @Override
    @Transactional
    public void unblock(UUID userId, UUID targetId) {
        BlockId id = new BlockId(userId, targetId);
        if (blockRepository.existsById(id)) {
            blockRepository.deleteById(id);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPage<UserSummaryResponse> listBlocked(UUID userId, String cursor, Integer limit) {
        int size = CursorPage.limit(limit);
        return CursorPage.of(userRepository.findBlockedBy(userId, Cursor.decode(cursor).id(), size + 1), size,
                user -> Cursor.ofId(user.getId()), userMapper::toSummaries);
    }
}
