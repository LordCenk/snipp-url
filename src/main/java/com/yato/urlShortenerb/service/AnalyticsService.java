package com.yato.urlShortenerb.service;

import com.yato.urlShortenerb.entity.Url;
import org.springframework.http.ResponseEntity;

public interface AnalyticsService {
    ResponseEntity<?> getAnalytics(String userEmail);
    void recordClick(Url url, String userAgent, String referrer);
}
