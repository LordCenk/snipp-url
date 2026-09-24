package com.yato.urlShortenerb.controller;


import com.yato.urlShortenerb.dto.UrlRequest;
import com.yato.urlShortenerb.service.UrlService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/urls")
@RequiredArgsConstructor
public class UrlController {

    private final UrlService urlService;


    private String currentUserEmail(){
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
    public ResponseEntity<?> getAll(@RequestParam(required = false) Integer page,
                                    @RequestParam(required = false) Integer size){
        return urlService.getAll(currentUserEmail(), page, size);
    }
    @Operation(summary = "Delete a URL")
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        return urlService.delete(id, currentUserEmail());
    }
    @Operation(summary = "Update an existing URL")
    @PostMapping("/update/{id}")
    public ResponseEntity<?> update(
            @PathVariable Long id,
            @RequestBody UrlRequest request) {
        return urlService.update(id, request, currentUserEmail());
    }


}
