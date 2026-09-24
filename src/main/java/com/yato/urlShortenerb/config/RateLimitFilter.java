package com.yato.urlShortenerb.config;

import com.yato.urlShortenerb.ratelimit.RateLimiter;
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

/**
 * Per-client-IP rate limiting for abuse-prone endpoints (login/register brute
 * force, link-creation spam). Counting is delegated to a {@link RateLimiter}:
 * in memory per instance by default, or shared through Redis when
 * APP_REDIS_ENABLED=true.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RateLimitFilter extends OncePerRequestFilter {

    private static final long WINDOW_MS = 60_000;

    private final RateLimiter rateLimiter;
    private final int authPerMinute;
    private final int createPerMinute;

    public RateLimitFilter(
            RateLimiter rateLimiter,
            @Value("${app.rate-limit.auth-per-minute:10}") int authPerMinute,
            @Value("${app.rate-limit.create-per-minute:30}") int createPerMinute) {
        this.rateLimiter = rateLimiter;
        this.authPerMinute = authPerMinute;
        this.createPerMinute = createPerMinute;
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
        RateLimiter.Decision decision =
                rateLimiter.tryAcquire(bucket + ":" + request.getRemoteAddr(), limit, WINDOW_MS);

        if (!decision.allowed()) {
            long retryAfterSeconds = Math.max(1, (decision.retryAfterMs() + 999) / 1000);
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
