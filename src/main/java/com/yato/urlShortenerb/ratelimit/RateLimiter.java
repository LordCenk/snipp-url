package com.yato.urlShortenerb.ratelimit;

/** Fixed-window request counter. */
public interface RateLimiter {

    /**
     * Counts one request against {@code key}.
     *
     * @return how the request fares against {@code limit} requests per {@code windowMs}
     */
    Decision tryAcquire(String key, int limit, long windowMs);

    record Decision(boolean allowed, long retryAfterMs) {
        public static Decision allow() {
            return new Decision(true, 0);
        }
    }
}
