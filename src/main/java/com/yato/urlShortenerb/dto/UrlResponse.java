package com.yato.urlShortenerb.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.yato.urlShortenerb.entity.Url;

import java.time.LocalDateTime;

public record UrlResponse(
        Long id,
        @JsonProperty("shortCode")
        String shortcode,
        @JsonProperty("longUrl")
        String longUrl,
        @JsonProperty("clickCount")
        Long clickcount,
        LocalDateTime expiry,
        LocalDateTime createdAt
) {
    public static UrlResponse from(Url url) {
        return new UrlResponse(url.getId(), url.getShortCode(), url.getLongUrl(), url.getClickCount(),
                url.getExpiry(), url.getCrtAt());
    }
}
