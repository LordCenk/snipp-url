package com.yato.urlShortenerb.service.impl;

import com.yato.urlShortenerb.cache.RedirectCache;
import com.yato.urlShortenerb.dto.UrlRequest;
import com.yato.urlShortenerb.dto.UrlResponse;
import com.yato.urlShortenerb.entity.Url;
import com.yato.urlShortenerb.entity.User;
import com.yato.urlShortenerb.repo.AnalyticsEventRepo;
import com.yato.urlShortenerb.repo.UrlRepo;
import com.yato.urlShortenerb.repo.UserRepo;
import com.yato.urlShortenerb.service.UrlService;
import com.yato.urlShortenerb.util.LogMasker;
import com.yato.urlShortenerb.util.ShortCodeGenerator;
import com.yato.urlShortenerb.util.UrlValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

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
    private final RedirectCache redirectCache;

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    @Override
    public ResponseEntity<?> create(UrlRequest request, String currentUserEmail) {
        log.debug("Creating short URL for user {}", LogMasker.maskEmail(currentUserEmail));

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

        User user = findUser(currentUserEmail);

        Url url = new Url();
        url.setUser(user);
        url.setLongUrl(request.longUrl().trim());
        url.setShortCode(generateUniqueCode());
        url.setCrtAt(LocalDateTime.now());
        url.setExpiry(expiry);

        urlRepo.save(url);

        log.info("Created short code {} (id {})", url.getShortCode(), url.getId());


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

    private User findUser(String email) {
        return userRepo.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    private Url findUrl(Long id) {
        return urlRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "URL not found"));
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
    public ResponseEntity<?> getAll(String currentUserEmail, Integer page, Integer size) {
        log.debug("Fetching URLs for user {}", LogMasker.maskEmail(currentUserEmail));

        User user = findUser(currentUserEmail);

        // No paging params: return everything, as before
        if (page == null && size == null) {
            List<UrlResponse> resp = urlRepo.findByUserId(user.getId())
                    .stream()
                    .map(this::toResponse)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(resp);
        }

        int pageNumber = page == null ? 0 : page;
        int pageSize = size == null ? DEFAULT_PAGE_SIZE : size;
        if (pageNumber < 0 || pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            return ResponseEntity.badRequest()
                    .body("page must be >= 0 and size between 1 and " + MAX_PAGE_SIZE);
        }

        // Body stays a plain array; the total is exposed in a header
        Page<Url> result = urlRepo.findByUserId(user.getId(),
                PageRequest.of(pageNumber, pageSize, Sort.by(Sort.Direction.DESC, "id")));
        return ResponseEntity.ok()
                .header("X-Total-Count", String.valueOf(result.getTotalElements()))
                .body(result.getContent().stream().map(this::toResponse).toList());
    }

    private UrlResponse toResponse(Url u) {
        return new UrlResponse(u.getId(), u.getShortCode(), u.getLongUrl(), u.getClickCount());
    }

    @Override
    @Transactional
    public ResponseEntity<?> delete(Long id, String currentUserEmail) {
        log.debug("Deleting URL {} for {}", id, LogMasker.maskEmail(currentUserEmail));

        User user = findUser(currentUserEmail);
        Url url = findUrl(id);

        if (!url.getUser().getId().equals(user.getId())) {
            log.warn("Forbidden delete attempt by {} for url {}", LogMasker.maskEmail(currentUserEmail), id);
            return ResponseEntity.status(403).body("Forbidden");
        }

        // Analytics events reference the URL via a foreign key, so remove them first
        analyticsRepo.deleteByUrl(url);
        urlRepo.delete(url);
        redirectCache.evict(url.getShortCode());
        log.info("URL {} deleted", id);

        return ResponseEntity.ok("Deleted");
    }
    @Override
    public ResponseEntity<?> update(Long id, UrlRequest request, String currentUserEmail) {

        User user = findUser(currentUserEmail);
        Url url = findUrl(id);

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
        redirectCache.evict(url.getShortCode());

        return ResponseEntity.ok("URL updated successfully");
    }

}
