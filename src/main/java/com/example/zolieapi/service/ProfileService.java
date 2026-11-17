package com.example.zolieapi.service;

import com.example.zolieapi.dto.response.ProfileResponse;
import java.util.UUID;

public interface ProfileService {

    ProfileResponse getByUsername(UUID viewerId, String username);
}
