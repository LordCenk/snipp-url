package com.yato.urlShortenerb.ratelimit;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Counters shared by every instance through Redis, so limits hold across a
 * horizontally scaled deployment.
 * <p>
 * If Redis is unreachable the request is allowed (fail open): an outage of an
 * optional component should not take the API down with it.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.redis.enabled", havingValue = "true")
public class RedisRateLimiter implements RateLimiter {

    static final String KEY_PREFIX = "snipp:ratelimit:";
    private static final long FAILURE_LOG_INTERVAL_MS = 60_000;

    // Atomic INCR + expiry on first hit; returns {count, ttl in ms}.
    // The PTTL guard restores an expiry if a key ever ends up without one.
    private static final RedisScript<List> SCRIPT = new DefaultRedisScript<>("""
            local count = redis.call('INCR', KEYS[1])
            if count == 1 then
                redis.call('PEXPIRE', KEYS[1], ARGV[1])
            end
            local ttl = redis.call('PTTL', KEYS[1])
            if ttl < 0 then
                redis.call('PEXPIRE', KEYS[1], ARGV[1])
                ttl = tonumber(ARGV[1])
            end
            return {count, ttl}
            """, List.class);

    private final StringRedisTemplate redis;
    private final AtomicLong lastFailureLog = new AtomicLong();

    public RedisRateLimiter(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public Decision tryAcquire(String key, int limit, long windowMs) {
        try {
            List<?> result = redis.execute(SCRIPT, List.of(KEY_PREFIX + key), String.valueOf(windowMs));
            long count = ((Number) result.get(0)).longValue();
            long ttl = ((Number) result.get(1)).longValue();
            return count > limit ? new Decision(false, ttl) : Decision.allow();
        } catch (RuntimeException e) {
            long now = System.currentTimeMillis();
            long last = lastFailureLog.get();
            if (now - last >= FAILURE_LOG_INTERVAL_MS && lastFailureLog.compareAndSet(last, now)) {
                log.warn("Rate limiting unavailable, allowing requests: {}", e.getMessage());
            }
            return Decision.allow();
        }
    }
}
