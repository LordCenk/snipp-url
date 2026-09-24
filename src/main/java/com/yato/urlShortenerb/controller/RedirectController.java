package com.yato.urlShortenerb.controller;

import com.yato.urlShortenerb.entity.Url;
import com.yato.urlShortenerb.repo.UrlRepo;
import com.yato.urlShortenerb.service.AnalyticsService;
import com.yato.urlShortenerb.util.UrlValidator;
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
    private final AnalyticsService analyticsService;

    @Operation(summary = "Redirect short code to original URL")
    @GetMapping("/s/{shortCode}")
    public ResponseEntity<?> redirect(@PathVariable String shortCode,
                                      HttpServletRequest request) {

        log.debug("Redirect request for {}", shortCode);

        Url url = urlRepo.findByShortCode(shortCode).orElse(null);

        if (url == null) {
            log.warn("Invalid short code {}", shortCode);
            return ResponseEntity.status(404).body("Short URL not found");
        }

        if (url.getExpiry() != null && url.getExpiry().isBefore(LocalDateTime.now())) {
            log.info("Expired short code {}", shortCode);
            return ResponseEntity.status(410).body("Short URL has expired");
        }

        // Guards against unsafe targets stored before creation-time validation existed
        if (!UrlValidator.isValidHttpUrl(url.getLongUrl())) {
            log.warn("Refusing redirect to unsafe URL for short code {}", shortCode);
            return ResponseEntity.badRequest().body("Invalid short URL");
        }

        analyticsService.recordClick(url, request.getHeader("User-Agent"), request.getHeader("Referer"));

        // Redirect user
        return ResponseEntity.status(302)
                .header("Location", url.getLongUrl())
                .build();
    }
}
