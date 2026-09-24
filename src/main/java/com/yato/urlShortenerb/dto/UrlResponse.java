package com.yato.urlShortenerb.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.yato.urlShortenerb.entity.Url;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * {@code expiry} and {@code createdAt} are exact instants in UTC (e.g.
 * "2026-12-31T18:29:00Z"), so clients can show them in the user's own time zone.
 */
public record UrlResponse(
        Long id,
        @JsonProperty("shortCode")
        String shortcode,
        @JsonProperty("longUrl")
        String longUrl,
        @JsonProperty("clickCount")
        Long clickcount,
        Instant expiry,
        Instant createdAt
) {
    public static UrlResponse from(Url url) {
        return new UrlResponse(url.getId(), url.getShortCode(), url.getLongUrl(), url.getClickCount(),
                toInstant(url.getExpiry()), toInstant(url.getCrtAt()));
    }

    // Stored timestamps are in the server's time zone
    private static Instant toInstant(LocalDateTime serverTime) {
        return serverTime == null ? null : serverTime.atZone(ZoneId.systemDefault()).toInstant();
    }
}
