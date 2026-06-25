package com.yato.urlShortenerb.service.impl;


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

        long refTotal = devices.stream().mapToLong(r -> (Long) r[1]).sum();
        if(refTotal > 0) {  // Only process if there's data
            for(Object[] row : devices){
                Map<String, Object> m = new HashMap<>();
                m.put("name", (String) row[0]);
                m.put("percentage", Math.round(((Long) row[1]) * 100.0 / refTotal));
                deviceStats.add(m);
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
        response.put("topUrl", topUrls);  // Can be null, that's ok
        response.put("dailyClicks", dailyClicks);
        response.put("devices", deviceStats);  // ✓ IMPORTANT: deviceStats not devices
        response.put("referrers", deviceStats);  // referrers is also deviceStats
        response.put("breakdown", breakdown);

        return ResponseEntity.ok(response);
    }
}
