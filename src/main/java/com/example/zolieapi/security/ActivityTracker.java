package com.example.zolieapi.security;

import com.example.zolieapi.repository.UserRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ActivityTracker {

    public static final Duration ONLINE_WINDOW = Duration.ofMinutes(5);

    private static final long WRITE_INTERVAL_MS = 60_000;

    private final UserRepository userRepository;
    private final Map<UUID, Long> lastWrites = new ConcurrentHashMap<>();

    public void touch(UUID userId) {
        long now = System.currentTimeMillis();
        Long previous = lastWrites.get(userId);
        if (previous != null && now - previous < WRITE_INTERVAL_MS) {
            return;
        }
        lastWrites.put(userId, now);
        userRepository.touchLastActive(userId);
    }

    public static boolean isOnline(Instant lastActiveAt) {
        return lastActiveAt != null && lastActiveAt.isAfter(Instant.now().minus(ONLINE_WINDOW));
    }

    @Scheduled(fixedDelay = 600_000)
    public void evictOld() {
        long cutoff = System.currentTimeMillis() - WRITE_INTERVAL_MS;
        lastWrites.values().removeIf(written -> written < cutoff);
    }
}
