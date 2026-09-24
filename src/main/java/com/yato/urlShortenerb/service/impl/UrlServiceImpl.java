package com.yato.urlShortenerb.service.impl;

import com.yato.urlShortenerb.dto.UrlRequest;
import com.yato.urlShortenerb.dto.UrlResponse;
import com.yato.urlShortenerb.entity.Url;
import com.yato.urlShortenerb.entity.User;
import com.yato.urlShortenerb.repo.AnalyticsEventRepo;
import com.yato.urlShortenerb.repo.UrlRepo;
import com.yato.urlShortenerb.repo.UserRepo;
import com.yato.urlShortenerb.service.UrlService;
import com.yato.urlShortenerb.util.ShortCodeGenerator;
import com.yato.urlShortenerb.util.UrlValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UrlServiceImpl implements UrlService {

    private final UrlRepo urlRepo;
    private final UserRepo userRepo;
    private final AnalyticsEventRepo analyticsRepo;

    @Override
    public ResponseEntity<?> create(UrlRequest request, String currentUserEmail) {
        log.info("Creating short URL for user {}", currentUserEmail);

        if (!UrlValidator.isValidHttpUrl(request.longUrl())) {
            return ResponseEntity.badRequest().body("Invalid URL: must be an absolute http(s) URL");
        }

        LocalDateTime expiry = null;
        if (request.expiry() != null && !request.expiry().isBlank()) {
            expiry = parseExpiry(request.expiry());
            if (expiry == null) {
                return ResponseEntity.badRequest().body("Invalid expiry format");
            }
        }

        User user = userRepo.findByEmail(currentUserEmail).orElseThrow();

        Url url = new Url();
        url.setUser(user);
        url.setLongUrl(request.longUrl().trim());
        url.setShortCode(generateUniqueCode());
        url.setCrtAt(LocalDateTime.now());
        url.setExpiry(expiry);

        urlRepo.save(url);

        log.info("Created short code {} for URL {}", url.getShortCode(), url.getLongUrl());


        return ResponseEntity.ok(
                new UrlResponse(url.getId(), url.getShortCode(), url.getLongUrl(), url.getClickCount())
        );
    }

    private String generateUniqueCode() {
        String code;
        do {
            code = ShortCodeGenerator.generate();
        } while (urlRepo.findByShortCode(code).isPresent());

        log.debug("Generated unique short code: {}", code);
        return code;
    }

    // ISO-8601 local date-time, e.g. 2026-01-31T23:59 or 2026-01-31T23:59:00
    private LocalDateTime parseExpiry(String value) {
        try {
            return LocalDateTime.parse(value.trim());
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    @Override
    public ResponseEntity<?> getAll(String currentUserEmail) {
        log.info("Fetching URLs for user {}", currentUserEmail);

        User user = userRepo.findByEmail(currentUserEmail).orElseThrow();

        List<UrlResponse> resp = urlRepo.findByUserId(user.getId())
                .stream()
                .map(u -> new UrlResponse(
                        u.getId(),
                        u.getShortCode(),
                        u.getLongUrl(),
                        u.getClickCount()
                ))
                .collect(Collectors.toList());

        return ResponseEntity.ok(resp);
    }

    @Override
    @Transactional
    public ResponseEntity<?> delete(Long id, String currentUserEmail) {
        log.info("Deleting URL {} for {}", id, currentUserEmail);

        if (currentUserEmail == null) {
            return ResponseEntity.status(401).body("Invalid or expired token");
        }

        User user = userRepo.findByEmail(currentUserEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Url url = urlRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("URL not found"));

        if (!url.getUser().getId().equals(user.getId())) {
            log.warn("Forbidden delete attempt by {} for url {}", currentUserEmail, id);
            return ResponseEntity.status(403).body("Forbidden");
        }

        // Analytics events reference the URL via a foreign key, so remove them first
        analyticsRepo.deleteByUrl(url);
        urlRepo.delete(url);
        log.info("URL {} deleted successfully by {}", id, currentUserEmail);

        return ResponseEntity.ok("Deleted");
    }
    @Override
    public ResponseEntity<?> update(Long id, UrlRequest request, String currentUserEmail) {

        User user = userRepo.findByEmail(currentUserEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Url url = urlRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("URL not found"));

        if (!url.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(403).body("Forbidden");
        }

        // Update only if longUrl is provided
        if (request.longUrl() != null && !request.longUrl().isBlank()) {
            if (!UrlValidator.isValidHttpUrl(request.longUrl())) {
                return ResponseEntity.badRequest().body("Invalid URL: must be an absolute http(s) URL");
            }
            url.setLongUrl(request.longUrl().trim());
        }

        // Handle expiry
        if (request.expiry() != null && !request.expiry().isBlank()) {
            LocalDateTime expiry = parseExpiry(request.expiry());
            if (expiry == null) {
                return ResponseEntity.badRequest().body("Invalid expiry format");
            }
            url.setExpiry(expiry);
        }

        urlRepo.save(url);

        return ResponseEntity.ok("URL updated successfully");
    }

}
