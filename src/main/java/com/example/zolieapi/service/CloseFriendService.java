package com.example.zolieapi.service;

import com.example.zolieapi.common.CursorPage;
import com.example.zolieapi.dto.response.UserSummaryResponse;
import java.util.UUID;

public interface CloseFriendService {

    void add(UUID userId, UUID friendId);

    void remove(UUID userId, UUID friendId);

    CursorPage<UserSummaryResponse> list(UUID userId, String cursor, Integer limit);
}
