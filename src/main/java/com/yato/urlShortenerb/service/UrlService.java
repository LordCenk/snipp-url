package com.yato.urlShortenerb.service;

import com.yato.urlShortenerb.dto.UrlRequest;
import org.springframework.http.ResponseEntity;

public interface UrlService {
    ResponseEntity<?> create(UrlRequest request, String currentUserEmail);
    ResponseEntity<?> getAll(String currentUserEmail);
    ResponseEntity<?> delete(Long id, String currentUserEmail);
    ResponseEntity<?> update(Long id, UrlRequest request, String currentUserEmail);
}
