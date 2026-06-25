package com.yato.urlShortenerb.service;

import com.yato.urlShortenerb.dto.LoginRequest;
import com.yato.urlShortenerb.dto.RegisterRequest;
import org.springframework.http.ResponseEntity;

public interface UserService {
    ResponseEntity<?> register(RegisterRequest request);
    ResponseEntity<?> login(LoginRequest request);
}
