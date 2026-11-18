package com.example.zolieapi.service;

import com.example.zolieapi.common.CursorPage;
import com.example.zolieapi.dto.response.HashtagResponse;
import com.example.zolieapi.dto.response.PostResponse;
import com.example.zolieapi.dto.response.ReelResponse;
import java.util.List;
import java.util.UUID;

public interface HashtagService {

    void tagPost(UUID postId, String text);

    void tagReel(UUID reelId, String text);

    List<HashtagResponse> trending();

    List<HashtagResponse> search(String query);

    CursorPage<PostResponse> listPosts(UUID viewerId, String tag, String cursor, Integer limit);

    CursorPage<ReelResponse> listReels(UUID viewerId, String tag, String cursor, Integer limit);
}
