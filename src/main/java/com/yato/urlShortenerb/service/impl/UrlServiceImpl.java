package com.yato.urlShortenerb.service.impl;

import com.yato.urlShortenerb.dto.UrlRequest;
import com.yato.urlShortenerb.dto.UrlResponse;
import com.yato.urlShortenerb.entity.Url;
import com.yato.urlShortenerb.entity.User;
import com.yato.urlShortenerb.repo.UrlRepo;
import com.yato.urlShortenerb.repo.UserRepo;
import com.yato.urlShortenerb.service.UrlService;
import com.yato.urlShortenerb.util.ShortCodeGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UrlServiceImpl implements UrlService {

    private final UrlRepo urlRepo;
    private final UserRepo userRepo;

    @Override
    public ResponseEntity<?> create(UrlRequest request, String currentUserEmail) {
        log.info("Creating short URL for user {}", currentUserEmail);

        User user = userRepo.findByEmail(currentUserEmail).orElseThrow();

        Url url = new Url();
        url.setUser(user);
        url.setLongUrl(request.longUrl());
        url.setShortCode(generateUniqueCode());
        url.setCrtAt(LocalDateTime.now());

// Apply expiry if provided
        if (request.expiry() != null && !request.expiry().isEmpty()) {
            url.setExpiry(LocalDateTime.parse(request.expiry()));
        }

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
            url.setLongUrl(request.longUrl());
        }

        // Handle expiry
        if (request.expiry() != null && !request.expiry().isBlank()) {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
                LocalDateTime expiry = LocalDateTime.parse(request.expiry(), formatter);
                url.setExpiry(expiry);
            } catch (Exception e) {
                return ResponseEntity.badRequest().body("Invalid expiry format");
            }
        }

        urlRepo.save(url);

        return ResponseEntity.ok("URL updated successfully");
    }

}
