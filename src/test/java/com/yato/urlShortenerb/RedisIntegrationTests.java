package com.yato.urlShortenerb;

import com.yato.urlShortenerb.ratelimit.RateLimiter;
import com.yato.urlShortenerb.ratelimit.RedisRateLimiter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.net.URI;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Runs with APP_REDIS_ENABLED=true against the Redis at REDIS_URL (set in CI).
 */
@SpringBootTest
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "REDIS_URL", matches = ".+")
@TestPropertySource(properties = {
        "app.redis.enabled=true",
        "app.rate-limit.auth-per-minute=3",
        "app.rate-limit.create-per-minute=1000"
})
class RedisIntegrationTests {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private RateLimiter rateLimiter;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private StringRedisTemplate redis;

    // Redis state outlives a test run, so each test uses a fresh client address
    private static String randomIp() {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        return "10." + r.nextInt(256) + "." + r.nextInt(256) + "." + r.nextInt(1, 255);
    }

    private static RequestPostProcessor from(String ip) {
        return request -> {
            request.setRemoteAddr(ip);
            return request;
        };
    }

    @Test
    void redisBackedImplementationsAreActive() {
        assertInstanceOf(RedisRateLimiter.class, rateLimiter);
        assertInstanceOf(RedisCacheManager.class, cacheManager);
    }

    @Test
    void rateLimitIsSharedAcrossInstances() {
        // A second limiter with its own Redis connection stands in for another app instance
        URI uri = URI.create(System.getenv("REDIS_URL"));
        LettuceConnectionFactory otherConnection =
                new LettuceConnectionFactory(new RedisStandaloneConfiguration(uri.getHost(), uri.getPort()));
        otherConnection.afterPropertiesSet();
        otherConnection.start();
        try {
            RedisRateLimiter otherInstance = new RedisRateLimiter(new StringRedisTemplate(otherConnection));
            String key = "test:" + UUID.randomUUID();

            assertTrue(rateLimiter.tryAcquire(key, 3, 60_000).allowed());
            assertTrue(otherInstance.tryAcquire(key, 3, 60_000).allowed());
            assertTrue(rateLimiter.tryAcquire(key, 3, 60_000).allowed());

            RateLimiter.Decision fourth = otherInstance.tryAcquire(key, 3, 60_000);
            assertFalse(fourth.allowed(), "4th request across two instances must be rejected");
            assertTrue(fourth.retryAfterMs() > 0 && fourth.retryAfterMs() <= 60_000);
        } finally {
            otherConnection.destroy();
        }
    }

    @Test
    void loginRateLimitIsStoredInRedis() throws Exception {
        String ip = randomIp();
        String body = "{\"email\":\"" + UUID.randomUUID() + "@test.com\",\"password\":\"secret123\"}";
        for (int i = 0; i < 3; i++) {
            mvc.perform(post("/auth/login").with(from(ip)).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isUnauthorized());
        }
        mvc.perform(post("/auth/login").with(from(ip)).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"));

        assertEquals("4", redis.opsForValue().get("snipp:ratelimit:auth:" + ip));
    }

    @Test
    void redirectCacheLivesInRedisAndIsEvictedOnUpdate() throws Exception {
        String ip = randomIp();
        String body = "{\"email\":\"" + UUID.randomUUID() + "@test.com\",\"password\":\"secret123\"}";
        mvc.perform(post("/auth/register").with(from(ip)).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());
        String token = mvc.perform(post("/auth/login").with(from(ip)).contentType(MediaType.APPLICATION_JSON).content(body))
                .andReturn().getResponse().getContentAsString().replaceAll(".*\"token\":\"([^\"]+)\".*", "$1");

        String created = mvc.perform(post("/urls/create").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"longUrl\":\"https://example.com/old\"}"))
                .andReturn().getResponse().getContentAsString();
        String id = created.replaceAll(".*\"id\":(\\d+).*", "$1");
        String code = created.replaceAll(".*\"shortCode\":\"([^\"]+)\".*", "$1");
        String cacheKey = "snipp:redirects::" + code;

        mvc.perform(get("/s/" + code)).andExpect(header().string("Location", "https://example.com/old"));
        assertTrue(redis.hasKey(cacheKey), "redirect should be cached in Redis");

        mvc.perform(post("/urls/update/" + id).header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"longUrl\":\"https://example.com/new\"}"))
                .andExpect(status().isOk());
        assertFalse(redis.hasKey(cacheKey), "update should evict the shared cache entry");

        mvc.perform(get("/s/" + code)).andExpect(header().string("Location", "https://example.com/new"));
    }
}
