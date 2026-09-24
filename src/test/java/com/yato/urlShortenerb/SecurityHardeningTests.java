package com.yato.urlShortenerb;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "app.rate-limit.auth-per-minute=3",
        "app.rate-limit.create-per-minute=2",
        "app.cors.allowed-origins=https://allowed.example,https://other.example"
})
class SecurityHardeningTests {

    @Autowired
    private MockMvc mvc;

    // Each test uses its own client address so rate-limit windows don't interfere
    private static RequestPostProcessor from(String ip) {
        return request -> {
            request.setRemoteAddr(ip);
            return request;
        };
    }

    private static String credentials() {
        return "{\"email\":\"" + UUID.randomUUID() + "@test.com\",\"password\":\"secret123\"}";
    }

    @Test
    void loginIsRateLimitedPerClient() throws Exception {
        String body = credentials();
        for (int i = 0; i < 3; i++) {
            mvc.perform(post("/auth/login").with(from("10.0.0.1"))
                            .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isUnauthorized());
        }
        mvc.perform(post("/auth/login").with(from("10.0.0.1"))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"));

        // A different client is not affected
        mvc.perform(post("/auth/login").with(from("10.0.0.2"))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void urlCreationIsRateLimited() throws Exception {
        String body = credentials();
        mvc.perform(post("/auth/register").with(from("10.0.1.1"))
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isOk());
        String login = mvc.perform(post("/auth/login").with(from("10.0.1.1"))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String token = login.replaceAll(".*\"token\":\"([^\"]+)\".*", "$1");

        for (int i = 0; i < 2; i++) {
            mvc.perform(post("/urls/create").with(from("10.0.1.1"))
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON).content("{\"longUrl\":\"https://example.com\"}"))
                    .andExpect(status().isOk());
        }
        mvc.perform(post("/urls/create").with(from("10.0.1.1"))
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"longUrl\":\"https://example.com\"}"))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void registrationRequiresPasswordOfAtLeastEightCharacters() throws Exception {
        mvc.perform(post("/auth/register").with(from("10.0.2.1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + UUID.randomUUID() + "@test.com\",\"password\":\"short\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void corsAllowsOnlyConfiguredOrigins() throws Exception {
        mvc.perform(options("/urls/create")
                        .header("Origin", "https://allowed.example")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "https://allowed.example"));

        mvc.perform(options("/urls/create")
                        .header("Origin", "https://evil.example")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }
}
