package com.yato.urlShortenerb.controller;


import com.yato.urlShortenerb.config.JWTUtils;
import com.yato.urlShortenerb.dto.UrlRequest;
import com.yato.urlShortenerb.service.UrlService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/urls")
@RequiredArgsConstructor
public class UrlController {

    private final UrlService urlService;
    private final JWTUtils jwtUtils;


    private String currentUserEmail(){
        log.info("AUTH = {}", SecurityContextHolder.getContext().getAuthentication());
        Object principal= SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if(principal instanceof UserDetails ud) return ud.getUsername();
        return principal.toString();
    }

    @Operation(summary = "Create a short URL")
    @ApiResponses({ @ApiResponse(responseCode = "200", description = "Created") })
    @PostMapping("/create")
    public ResponseEntity<?> create(@RequestBody UrlRequest request){
        return urlService.create(request, currentUserEmail());
    }

    @Operation(summary="Get all user's URLs")
    @GetMapping("/all")
    public ResponseEntity<?> getAll(){
        return urlService.getAll(currentUserEmail());
    }
    @Operation(summary = "Delete a URL")
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id, HttpServletRequest request) {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body("Missing or invalid token");
        }

        String token = authHeader.substring(7);

        // FIX: Correct method name
        String email = jwtUtils.getEmailFromToken(token);

        if (email == null) {
            return ResponseEntity.status(401).body("Invalid or expired token");
        }

        return urlService.delete(id, email);
    }
    @Operation(summary = "Update an existing URL")
    @PostMapping("/update/{id}")
    public ResponseEntity<?> update(
            @PathVariable Long id,
            @RequestBody UrlRequest request,
            HttpServletRequest httpRequest) {

        String authHeader = httpRequest.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body("Missing or invalid token");
        }

        String token = authHeader.substring(7);
        String email = jwtUtils.getEmailFromToken(token);

        if (email == null) {
            return ResponseEntity.status(401).body("Invalid or expired token");
        }

        return urlService.update(id, request, email);
    }


}
