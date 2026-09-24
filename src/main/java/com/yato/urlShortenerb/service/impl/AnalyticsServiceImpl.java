package com.yato.urlShortenerb.service.impl;


import com.yato.urlShortenerb.dto.UrlResponse;
import com.yato.urlShortenerb.entity.Url;
import com.yato.urlShortenerb.repo.AnalyticsEventRepo;
import com.yato.urlShortenerb.repo.UrlRepo;
import com.yato.urlShortenerb.repo.UserRepo;
import com.yato.urlShortenerb.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class AnalyticsServiceImpl implements AnalyticsService {

    private final UserRepo userRepo;
    private final UrlRepo urlRepo;
    private final AnalyticsEventRepo analyticsRepo;

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<?> getAnalytics(String userEmail){
        var user = userRepo.findByEmail(userEmail).orElse(null);
        if(user == null)
            return ResponseEntity.status(401).body("Invalid user");

        List<Url> urls = urlRepo.findByUserId(user.getId());

        long totalClicks = analyticsRepo.countByUrl(urls);
        long totalUrls = urls.size();

        Url topUrls = urls.stream()
                .max(Comparator.comparingLong(u -> u.getClickCount() == null ? 0 : u.getClickCount()))
                .orElse(null);

        List<Object[]> daily = analyticsRepo.countClicksPerDay(urls);
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

        List<Object[]> devices = analyticsRepo.countDevices(urls);
        List<Map<String,Object>> deviceStats = new ArrayList<>();

        long deviceTotal = devices.stream().mapToLong(r -> (Long) r[1]).sum();
        if(deviceTotal > 0) {  // Only process if there's data
            for(Object[] row : devices){
                Map<String, Object> m = new HashMap<>();
                m.put("name", row[0] == null ? "Unknown" : (String) row[0]);
                m.put("percentage", Math.round(((Long) row[1]) * 100.0 / deviceTotal));
                deviceStats.add(m);
            }
        }

        List<Object[]> referrers = analyticsRepo.countReferrers(urls);
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
