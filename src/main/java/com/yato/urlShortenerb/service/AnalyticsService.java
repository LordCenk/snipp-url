package com.yato.urlShortenerb.service;

import org.springframework.http.ResponseEntity;

public interface AnalyticsService {
    ResponseEntity<?> getAnalytics(String userEmail);
}
