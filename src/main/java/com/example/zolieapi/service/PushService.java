package com.example.zolieapi.service;

import java.util.Map;
import java.util.UUID;

public interface PushService {

    void send(UUID userId, Map<String, String> data);
}
