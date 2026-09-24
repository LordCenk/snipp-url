package com.yato.urlShortenerb.ratelimit;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/** Per-instance counters. The default when Redis is not enabled. */
@Component
@ConditionalOnProperty(name = "app.redis.enabled", havingValue = "false", matchIfMissing = true)
public class InMemoryRateLimiter implements RateLimiter {

    private static final int CLEANUP_EVERY = 1_000;

    private final Map<String, Window> windows = new ConcurrentHashMap<>();
    private final AtomicInteger requestCount = new AtomicInteger();

    private record Window(long start, AtomicInteger count) {
    }

    @Override
    public Decision tryAcquire(String key, int limit, long windowMs) {
        long now = System.currentTimeMillis();
        Window window = windows.compute(key, (k, w) ->
                w == null || now - w.start() >= windowMs ? new Window(now, new AtomicInteger()) : w);

        if (requestCount.incrementAndGet() % CLEANUP_EVERY == 0) {
            windows.entrySet().removeIf(e -> now - e.getValue().start() >= windowMs);
        }

        if (window.count().incrementAndGet() > limit) {
            return new Decision(false, window.start() + windowMs - now);
        }
        return Decision.allow();
    }
}
