package com.example.zolieapi.service.impl;

import com.example.zolieapi.common.Cursor;
import com.example.zolieapi.common.CursorPage;
import com.example.zolieapi.common.Texts;
import com.example.zolieapi.exception.ApiException;
import com.example.zolieapi.mapper.PostMapper;
import com.example.zolieapi.mapper.ReelMapper;
import com.example.zolieapi.mapper.UserMapper;
import com.example.zolieapi.repository.PostRepository;
import com.example.zolieapi.repository.ReelRepository;
import com.example.zolieapi.repository.UserRepository;
import com.example.zolieapi.security.RateLimiter;
import com.example.zolieapi.service.SearchService;
import java.time.Duration;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

    private static final int MAX_QUERY = 50;

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final ReelRepository reelRepository;
    private final RateLimiter rateLimiter;
    private final UserMapper userMapper;
    private final PostMapper postMapper;
    private final ReelMapper reelMapper;

    @Override
    @Transactional(readOnly = true)
    public CursorPage<?> search(UUID viewerId, String query, String type, String cursor, Integer limit) {
        rateLimiter.check("search", viewerId, 60, Duration.ofMinutes(1));
        String text = Texts.clean(query, MAX_QUERY, "q");
        if (text == null) {
            return CursorPage.empty();
        }
        String pattern = "%" + text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
        int size = CursorPage.limit(limit);
        Cursor from = Cursor.decode(cursor);
        return switch (type == null ? "users" : type.toLowerCase()) {
            case "users" -> CursorPage.of(userRepository.search(pattern, viewerId, from.id(), size + 1), size,
                    user -> Cursor.ofId(user.getId()), userMapper::toSummaries);
            case "posts" -> CursorPage.of(postRepository.search(pattern, viewerId, from.time(), from.id(), size + 1),
                    size, post -> new Cursor(post.getCreatedAt(), post.getId()),
                    page -> postMapper.toResponses(viewerId, page));
            case "reels" -> CursorPage.of(reelRepository.search(pattern, viewerId, from.time(), from.id(), size + 1),
                    size, reel -> new Cursor(reel.getCreatedAt(), reel.getId()),
                    page -> reelMapper.toResponses(viewerId, page));
            default -> throw ApiException.invalid("type must be one of: users, posts, reels");
        };
    }
}
