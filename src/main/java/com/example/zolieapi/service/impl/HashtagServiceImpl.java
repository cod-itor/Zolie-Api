package com.example.zolieapi.service.impl;

import com.example.zolieapi.common.Cursor;
import com.example.zolieapi.common.CursorPage;
import com.example.zolieapi.common.Texts;
import com.example.zolieapi.dto.response.HashtagResponse;
import com.example.zolieapi.dto.response.PostResponse;
import com.example.zolieapi.dto.response.ReelResponse;
import com.example.zolieapi.mapper.PostMapper;
import com.example.zolieapi.mapper.ReelMapper;
import com.example.zolieapi.model.Hashtag;
import com.example.zolieapi.repository.HashtagRepository;
import com.example.zolieapi.repository.HashtagRepository.TagCountRow;
import com.example.zolieapi.repository.PostRepository;
import com.example.zolieapi.repository.ReelRepository;
import com.example.zolieapi.service.HashtagService;
import java.text.Normalizer;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class HashtagServiceImpl implements HashtagService {

    private static final Pattern HASHTAG = Pattern.compile("#([\\p{L}\\p{M}\\p{N}_]{1,100})");
    private static final int MAX_TAGS = 20;
    private static final int TRENDING_SIZE = 10;
    private static final int SEARCH_SIZE = 20;
    private static final int MAX_QUERY = 50;

    private final HashtagRepository hashtagRepository;
    private final PostRepository postRepository;
    private final ReelRepository reelRepository;
    private final PostMapper postMapper;
    private final ReelMapper reelMapper;

    @Override
    @Transactional
    public void tagPost(UUID postId, String text) {
        hashtagRepository.clearPost(postId);
        resolve(text).forEach(hashtag -> hashtagRepository.tagPost(postId, hashtag.getId()));
    }

    @Override
    @Transactional
    public void tagReel(UUID reelId, String text) {
        resolve(text).forEach(hashtag -> hashtagRepository.tagReel(reelId, hashtag.getId()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<HashtagResponse> trending() {
        return toResponses(hashtagRepository.findTrending(TRENDING_SIZE));
    }

    @Override
    @Transactional(readOnly = true)
    public List<HashtagResponse> search(String query) {
        String text = Texts.clean(query, MAX_QUERY, "q");
        if (text == null) {
            return trending();
        }
        return toResponses(hashtagRepository.search(Texts.likePattern(normalize(text.replace("#", ""))), SEARCH_SIZE));
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPage<PostResponse> listPosts(UUID viewerId, String tag, String cursor, Integer limit) {
        int size = CursorPage.limit(limit);
        Cursor from = Cursor.decode(cursor);
        return CursorPage.of(postRepository.findByHashtag(normalize(tag), viewerId, from.time(), from.id(), size + 1),
                size, post -> new Cursor(post.getCreatedAt(), post.getId()),
                page -> postMapper.toResponses(viewerId, page));
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPage<ReelResponse> listReels(UUID viewerId, String tag, String cursor, Integer limit) {
        int size = CursorPage.limit(limit);
        Cursor from = Cursor.decode(cursor);
        return CursorPage.of(reelRepository.findByHashtag(normalize(tag), viewerId, from.time(), from.id(), size + 1),
                size, reel -> new Cursor(reel.getCreatedAt(), reel.getId()),
                page -> reelMapper.toResponses(viewerId, page));
    }

    private List<Hashtag> resolve(String text) {
        if (text == null) {
            return List.of();
        }
        Set<String> tags = new LinkedHashSet<>();
        Matcher matcher = HASHTAG.matcher(text);
        while (matcher.find() && tags.size() < MAX_TAGS) {
            tags.add(normalize(matcher.group(1)));
        }
        if (tags.isEmpty()) {
            return List.of();
        }
        tags.forEach(hashtagRepository::insertIfAbsent);
        return hashtagRepository.findByTagIn(tags);
    }

    private String normalize(String tag) {
        return Normalizer.normalize(tag.trim(), Normalizer.Form.NFC).toLowerCase();
    }

    private List<HashtagResponse> toResponses(List<TagCountRow> rows) {
        return rows.stream().map(row -> new HashtagResponse(row.getTag(), row.getTotal())).toList();
    }
}
