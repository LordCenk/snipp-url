package com.yato.urlShortenerb.service.impl;


import com.yato.urlShortenerb.dto.UrlResponse;
import com.yato.urlShortenerb.entity.AnalyticsEvent;
import com.yato.urlShortenerb.entity.Url;
import com.yato.urlShortenerb.repo.AnalyticsEventRepo;
import com.yato.urlShortenerb.repo.UrlRepo;
import com.yato.urlShortenerb.repo.UserRepo;
import com.yato.urlShortenerb.service.AnalyticsService;
import com.yato.urlShortenerb.util.DeviceClassifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class AnalyticsServiceImpl implements AnalyticsService {

    private final UserRepo userRepo;
    private final UrlRepo urlRepo;
    private final AnalyticsEventRepo analyticsRepo;

    // Bounds what a client can make us store per click
    private static final int MAX_HEADER_LENGTH = 1024;

    @Override
    @Transactional
    public void recordClick(Url url, String userAgent, String referrer) {
        urlRepo.incrementClickCount(url.getId());

        AnalyticsEvent event = new AnalyticsEvent();
        event.setDevice(truncate(userAgent));
        event.setReferrer(truncate(referrer));
        event.setTimestamp(LocalDateTime.now());
        event.setUrl(url);
        analyticsRepo.save(event);
    }

    private static String truncate(String value) {
        return value == null || value.length() <= MAX_HEADER_LENGTH ? value : value.substring(0, MAX_HEADER_LENGTH);
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<?> getAnalytics(String userEmail){
        var user = userRepo.findByEmail(userEmail).orElse(null);
        if(user == null)
            return ResponseEntity.status(401).body("Invalid user");

        List<Url> urls = urlRepo.findByUserId(user.getId());

        long totalClicks = analyticsRepo.countByUserId(user.getId());
        long totalUrls = urls.size();

        Url topUrls = urls.stream()
                .max(Comparator.comparingLong(u -> u.getClickCount() == null ? 0 : u.getClickCount()))
                .orElse(null);

        List<Object[]> daily = analyticsRepo.countClicksPerDay(user.getId());
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM d");

        List<Map<String, Object>> dailyClicks = new ArrayList<>();
        for(Object[] row : daily){
            try {
                Map<String, Object> m = new HashMap<>();
                if(row[0] != null) {
                    m.put("date", fmt.format(((java.sql.Date) row[0]).toLocalDate()));
                    m.put("clicks", ((Long) row[1]).intValue());
                    dailyClicks.add(m);
                }
            } catch(Exception e) {
                log.error("Error processing daily click row", e);
            }
        }

        List<Object[]> devices = analyticsRepo.countDevices(user.getId());
        List<Map<String,Object>> deviceStats = new ArrayList<>();

        // Group raw User-Agent strings into device types (Mobile, Desktop, ...).
        // Done at read time, so clicks recorded before this change are grouped too.
        Map<String, Long> countsByType = new HashMap<>();
        for(Object[] row : devices){
            countsByType.merge(DeviceClassifier.classify((String) row[0]), (Long) row[1], Long::sum);
        }

        long deviceTotal = countsByType.values().stream().mapToLong(Long::longValue).sum();
        if(deviceTotal > 0) {  // Only process if there's data
            countsByType.entrySet().stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue().reversed()
                            .thenComparing(Map.Entry.comparingByKey()))
                    .forEach(e -> {
                        Map<String, Object> m = new HashMap<>();
                        m.put("name", e.getKey());
                        m.put("percentage", Math.round(e.getValue() * 100.0 / deviceTotal));
                        deviceStats.add(m);
                    });
        }

        List<Object[]> referrers = analyticsRepo.countReferrers(user.getId());
        List<Map<String,Object>> referrerStats = new ArrayList<>();

        long refTotal = referrers.stream().mapToLong(r -> (Long) r[1]).sum();
        if(refTotal > 0) {
            for(Object[] row : referrers){
                Map<String, Object> m = new HashMap<>();
                // No Referer header means the link was opened directly (typed, bookmarked, app)
                m.put("name", row[0] == null ? "Direct" : (String) row[0]);
                m.put("percentage", Math.round(((Long) row[1]) * 100.0 / refTotal));
                referrerStats.add(m);
            }
        }

        List<Map<String,Object>> breakdown = new ArrayList<>();
        for(Url u : urls){
            Map<String, Object> m = new HashMap<>();
            m.put("id", u.getId());
            m.put("shortCode", u.getShortCode());
            m.put("longUrl", u.getLongUrl());
            m.put("clickCount", u.getClickCount() != null ? u.getClickCount() : 0);
            breakdown.add(m);
        }

        Map<String,Object> response = new HashMap<>();
        response.put("totalClicks", totalClicks);
        response.put("totalUrls", totalUrls);
        // Map to a DTO: serializing the entity would expose its User (including the password hash)
        response.put("topUrl", topUrls == null ? null : new UrlResponse(
                topUrls.getId(), topUrls.getShortCode(), topUrls.getLongUrl(), topUrls.getClickCount()));
        response.put("dailyClicks", dailyClicks);
        response.put("devices", deviceStats);
        response.put("referrers", referrerStats);
        response.put("breakdown", breakdown);

        return ResponseEntity.ok(response);
    }
}
