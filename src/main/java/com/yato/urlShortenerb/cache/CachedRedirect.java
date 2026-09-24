package com.yato.urlShortenerb.cache;

import java.io.Serializable;
import java.time.LocalDateTime;

/** What a redirect needs to know about a short link; small and serializable so it can be cached anywhere. */
public record CachedRedirect(Long urlId, String longUrl, LocalDateTime expiry) implements Serializable {
}
