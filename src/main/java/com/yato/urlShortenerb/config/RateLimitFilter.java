package com.yato.urlShortenerb.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Fixed-window, per-client-IP rate limiting for abuse-prone endpoints
 * (login/register brute force, link-creation spam).
 * <p>
 * State is in memory, so limits apply per instance. When running several
 * instances, move this to a shared store (e.g. Redis) or the API gateway.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RateLimitFilter extends OncePerRequestFilter {

    private static final long WINDOW_MS = 60_000;
    private static final int CLEANUP_EVERY = 1_000;

    private final int authPerMinute;
    private final int createPerMinute;
    private final Map<String, Window> windows = new ConcurrentHashMap<>();
    private final AtomicInteger requestCount = new AtomicInteger();

    public RateLimitFilter(
            @Value("${app.rate-limit.auth-per-minute:10}") int authPerMinute,
            @Value("${app.rate-limit.create-per-minute:30}") int createPerMinute) {
        this.authPerMinute = authPerMinute;
        this.createPerMinute = createPerMinute;
    }

    private record Window(long start, AtomicInteger count) {
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String bucket = bucketFor(request);
        if (bucket == null) {
            filterChain.doFilter(request, response);
            return;
        }

        int limit = bucket.equals("auth") ? authPerMinute : createPerMinute;
        long now = System.currentTimeMillis();
        String key = bucket + ":" + request.getRemoteAddr();

        Window window = windows.compute(key, (k, w) ->
                w == null || now - w.start() >= WINDOW_MS ? new Window(now, new AtomicInteger()) : w);

        if (requestCount.incrementAndGet() % CLEANUP_EVERY == 0) {
            windows.entrySet().removeIf(e -> now - e.getValue().start() >= WINDOW_MS);
        }

        if (window.count().incrementAndGet() > limit) {
            long retryAfterSeconds = Math.max(1, (window.start() + WINDOW_MS - now + 999) / 1000);
            log.warn("Rate limit exceeded for {} on {}", request.getRemoteAddr(), bucket);
            response.setStatus(429);
            response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
            response.setContentType("text/plain;charset=UTF-8");
            response.getWriter().write("Too many requests, try again later");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private static String bucketFor(HttpServletRequest request) {
        if (!"POST".equalsIgnoreCase(request.getMethod())) return null;
        String path = request.getRequestURI();
        if (path.equals("/auth/login") || path.equals("/auth/register")) return "auth";
        if (path.equals("/urls/create")) return "create";
        return null;
    }
}
