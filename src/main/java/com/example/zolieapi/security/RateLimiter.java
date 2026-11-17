package com.example.zolieapi.security;

import com.example.zolieapi.exception.ApiException;
import com.example.zolieapi.exception.ErrorCode;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class RateLimiter {

    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    public void check(String action, Object subject, int limit, Duration period) {
        long now = System.currentTimeMillis();
        Window window = windows.compute(action + ":" + subject, (key, current) -> {
            if (current == null || now >= current.resetAt) {
                return new Window(now + period.toMillis(), 1);
            }
            return new Window(current.resetAt, current.count + 1);
        });
        if (window.count > limit) {
            long retryAfter = Math.max(1, (window.resetAt - now) / 1000);
            throw new ApiException(ErrorCode.RATE_LIMITED, ErrorCode.RATE_LIMITED.getDefaultMessage(), retryAfter);
        }
    }

    @Scheduled(fixedDelay = 300_000)
    public void evictExpired() {
        long now = System.currentTimeMillis();
        windows.values().removeIf(window -> now >= window.resetAt);
    }

    private record Window(long resetAt, int count) {
    }
}
