package com.yato.urlShortenerb.cache;

import com.yato.urlShortenerb.repo.UrlRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Short code -> redirect target lookups, cached so repeat clicks skip the database read.
 * Entries are evicted whenever a link is updated or deleted.
 */
@Component
@RequiredArgsConstructor
public class RedirectCache {

    public static final String CACHE_NAME = "redirects";

    private final UrlRepo urlRepo;
    private final CacheManager cacheManager;

    // Unknown codes are not cached, so random lookups can't fill the cache
    @Cacheable(cacheNames = CACHE_NAME, key = "#shortCode", unless = "#result == null")
    public CachedRedirect find(String shortCode) {
        return urlRepo.findByShortCode(shortCode)
                .map(u -> new CachedRedirect(u.getId(), u.getLongUrl(), u.getExpiry()))
                .orElse(null);
    }

    public void evict(String shortCode) {
        Runnable evict = () -> {
            Cache cache = cacheManager.getCache(CACHE_NAME);
            if (cache != null) cache.evict(shortCode);
        };
        evict.run();
        // Inside a transaction, evict again after commit so a redirect that ran
        // in between can't leave the old value cached
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    evict.run();
                }
            });
        }
    }
}
