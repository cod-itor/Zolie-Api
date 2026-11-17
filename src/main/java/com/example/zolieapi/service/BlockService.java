package com.example.zolieapi.service;

import com.example.zolieapi.common.CursorPage;
import com.example.zolieapi.dto.response.UserSummaryResponse;
import java.util.UUID;

public interface BlockService {

    void block(UUID userId, UUID targetId);

    void unblock(UUID userId, UUID targetId);

    CursorPage<UserSummaryResponse> listBlocked(UUID userId, String cursor, Integer limit);
}
