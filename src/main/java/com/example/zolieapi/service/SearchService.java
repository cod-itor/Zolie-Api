package com.example.zolieapi.service;

import com.example.zolieapi.common.CursorPage;
import java.util.UUID;

public interface SearchService {

    CursorPage<?> search(UUID viewerId, String query, String type, String cursor, Integer limit);
}
