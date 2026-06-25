package com.yato.urlShortenerb.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record UrlResponse(
        Long id,
        @JsonProperty("shortCode")
        String shortcode,
        @JsonProperty("longUrl")
        String longUrl,
        @JsonProperty("clickCount")
        Long clickcount
) {}
