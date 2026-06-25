package com.yato.urlShortenerb.controller;

import com.yato.urlShortenerb.entity.AnalyticsEvent;
import com.yato.urlShortenerb.entity.Url;
import com.yato.urlShortenerb.repo.AnalyticsEventRepo;
import com.yato.urlShortenerb.repo.UrlRepo;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@Slf4j
@RestController
@RequiredArgsConstructor
public class RedirectController {

    private final UrlRepo urlRepo;
    private final AnalyticsEventRepo analyticsRepo;

    @Operation(summary = "Redirect short code to original URL")
    @GetMapping("/s/{shortCode}")
    public ResponseEntity<?> redirect(@PathVariable String shortCode,
                                      HttpServletRequest request) {

        log.info("Redirect request for {}", shortCode);

        Url url = urlRepo.findByShortCode(shortCode).orElse(null);

        if (url == null) {
            log.warn("Invalid short code {}", shortCode);
            return ResponseEntity.badRequest().body("Invalid short URL");
        }

        // Increment click count
        url.setClickCount(url.getClickCount() + 1);
        urlRepo.save(url);

        // 🔥 Record analytics event BEFORE redirect
        AnalyticsEvent event = new AnalyticsEvent();
        event.setDevice(request.getHeader("User-Agent"));
        event.setReferrer(request.getHeader("Referer"));
        event.setTimestamp(LocalDateTime.now());
        event.setUrl(url);

        analyticsRepo.save(event);

        // Redirect user
        return ResponseEntity.status(302)
                .header("Location", url.getLongUrl())
                .build();
    }
}
