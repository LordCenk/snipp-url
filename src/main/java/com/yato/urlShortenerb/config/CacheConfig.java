package com.yato.urlShortenerb.config;

import com.yato.urlShortenerb.cache.RedirectCache;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.LoggingCacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import java.time.Duration;

@Configuration
@EnableCaching
public class CacheConfig implements CachingConfigurer {

    // A cache failure (e.g. Redis unreachable) is logged and treated as a miss,
    // so requests fall back to the database instead of failing
    @Override
    public CacheErrorHandler errorHandler() {
        return new LoggingCacheErrorHandler(false);
    }

    /**
     * With Redis enabled, the redirect cache is shared by all instances, so an
     * update or delete on one instance evicts the entry everywhere. Otherwise
     * the in-memory Caffeine cache configured in application.properties is used.
     */
    @Configuration
    @ConditionalOnProperty(name = "app.redis.enabled", havingValue = "true")
    static class RedisCacheConfig {

        @Bean
        CacheManager cacheManager(RedisConnectionFactory connectionFactory,
                                  @Value("${app.redis.cache-ttl:10m}") Duration ttl) {
            RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                    .entryTtl(ttl)
                    .prefixCacheNameWith("snipp:")
                    .disableCachingNullValues();
            return RedisCacheManager.builder(connectionFactory)
                    .cacheDefaults(config)
                    .initialCacheNames(java.util.Set.of(RedirectCache.CACHE_NAME))
                    .build();
        }
    }
}
