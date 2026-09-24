package com.yato.urlShortenerb;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
// Every test registers and logs in from the same address; rate limiting is covered in SecurityHardeningTests
@TestPropertySource(properties = {
        "app.rate-limit.auth-per-minute=10000",
        "app.rate-limit.create-per-minute=10000"
})
class UrlFlowIntegrationTests {

    @Autowired
    private MockMvc mvc;

    private String token;

    @BeforeEach
    void registerAndLogin() throws Exception {
        String body = "{\"email\":\"" + UUID.randomUUID() + "@test.com\",\"password\":\"secret123\"}";
        mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());
        String login = mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        token = extract(login, "token");
    }

    private String createUrl(String json) throws Exception {
        return mvc.perform(post("/urls/create")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    private static String extract(String json, String field) {
        Matcher m = Pattern.compile("\"" + field + "\":\"?([^\",}]+)").matcher(json);
        if (!m.find()) throw new AssertionError(field + " not found in " + json);
        return m.group(1);
    }

    @Test
    void analyticsDoesNotExposePasswordHash() throws Exception {
        String code = extract(createUrl("{\"longUrl\":\"https://example.com\"}"), "shortCode");
        mvc.perform(get("/s/" + code)).andExpect(status().isFound());

        mvc.perform(get("/analytics/overview").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.topUrl.shortCode").value(code))
                .andExpect(content().string(not(containsString("password"))))
                .andExpect(content().string(not(containsString("$2a$"))));
    }

    @Test
    void rejectsNonHttpUrls() throws Exception {
        for (String bad : new String[]{"javascript:alert(1)", "data:text/html,hi", "file:///etc/passwd", "not a url", ""}) {
            mvc.perform(post("/urls/create")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"longUrl\":\"" + bad + "\"}"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    void rejectsInvalidExpiry() throws Exception {
        mvc.perform(post("/urls/create")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"longUrl\":\"https://example.com\",\"expiry\":\"tomorrow\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void expiredUrlReturnsGone() throws Exception {
        String expired = extract(createUrl("{\"longUrl\":\"https://example.com\",\"expiry\":\"2020-01-01T00:00\"}"), "shortCode");
        mvc.perform(get("/s/" + expired)).andExpect(status().isGone());

        String future = extract(createUrl("{\"longUrl\":\"https://example.com\",\"expiry\":\"2999-01-01T00:00:00\"}"), "shortCode");
        mvc.perform(get("/s/" + future))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://example.com"));
    }

    @Test
    void canDeleteUrlThatHasClicks() throws Exception {
        String created = createUrl("{\"longUrl\":\"https://example.com\"}");
        String id = extract(created, "id");
        String code = extract(created, "shortCode");
        mvc.perform(get("/s/" + code)).andExpect(status().isFound());

        mvc.perform(delete("/urls/delete/" + id).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        mvc.perform(get("/s/" + code)).andExpect(status().isNotFound());
    }

    @Test
    void healthEndpointIsPublic() throws Exception {
        mvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
        mvc.perform(get("/api/health/readiness")).andExpect(status().isOk());
    }

    @Test
    void invalidRegistrationReturnsBadRequest() throws Exception {
        for (String body : new String[]{"{}", "{\"email\":\"not-an-email\",\"password\":\"secret123\"}",
                "{\"email\":\"x@test.com\",\"password\":\"\"}", "{not json"}) {
            mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    void missingOrInvalidTokenReturnsUnauthorized() throws Exception {
        mvc.perform(get("/urls/all")).andExpect(status().isUnauthorized());
        mvc.perform(get("/urls/all").header("Authorization", "Bearer garbage")).andExpect(status().isUnauthorized());
        mvc.perform(delete("/urls/delete/1").header("Authorization", "Bearer garbage")).andExpect(status().isUnauthorized());
    }

    @Test
    void missingResourcesReturnNotFound() throws Exception {
        mvc.perform(delete("/urls/delete/999999").header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
        mvc.perform(post("/urls/update/999999").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"longUrl\":\"https://example.com\"}"))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/urls/delete/abc").header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/s/doesNotExist")).andExpect(status().isNotFound());
    }

    @Test
    void analyticsReportsReferrersSeparatelyFromDevices() throws Exception {
        String code = extract(createUrl("{\"longUrl\":\"https://example.com\"}"), "shortCode");
        mvc.perform(get("/s/" + code).header("User-Agent", "TestAgent").header("Referer", "https://twitter.com/"))
                .andExpect(status().isFound());
        mvc.perform(get("/s/" + code).header("User-Agent", "TestAgent"))
                .andExpect(status().isFound());

        mvc.perform(get("/analytics/overview").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.devices[0].name").value("TestAgent"))
                .andExpect(jsonPath("$.referrers[?(@.name == 'https://twitter.com/')].percentage").value(50))
                .andExpect(jsonPath("$.referrers[?(@.name == 'Direct')].percentage").value(50));
    }

    @Test
    void concurrentClicksAreAllCounted() throws Exception {
        String code = extract(createUrl("{\"longUrl\":\"https://example.com\"}"), "shortCode");
        int clicks = 40;

        ExecutorService pool = Executors.newFixedThreadPool(8);
        try {
            List<Future<Integer>> results = new ArrayList<>();
            for (int i = 0; i < clicks; i++) {
                results.add(pool.submit(() -> mvc.perform(get("/s/" + code)).andReturn().getResponse().getStatus()));
            }
            for (Future<Integer> r : results) {
                org.junit.jupiter.api.Assertions.assertEquals(302, r.get());
            }
        } finally {
            pool.shutdown();
        }

        mvc.perform(get("/urls/all").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$[0].clickCount").value(clicks));
        mvc.perform(get("/analytics/overview").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.totalClicks").value(clicks));
    }

    @Test
    void longUserAgentAndReferrerDoNotBreakRedirect() throws Exception {
        String code = extract(createUrl("{\"longUrl\":\"https://example.com\"}"), "shortCode");
        String longValue = "https://example.com/" + "a".repeat(5000);

        mvc.perform(get("/s/" + code).header("User-Agent", longValue).header("Referer", longValue))
                .andExpect(status().isFound());
        mvc.perform(get("/analytics/overview").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.totalClicks").value(1));
    }

    @Test
    void urlListSupportsOptionalPagination() throws Exception {
        for (int i = 1; i <= 3; i++) {
            createUrl("{\"longUrl\":\"https://example.com/" + i + "\"}");
        }

        // Without params: full list, unchanged response shape
        mvc.perform(get("/urls/all").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));

        // Paged: newest first, total in header
        mvc.perform(get("/urls/all?page=0&size=2").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Total-Count", "3"))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].longUrl").value("https://example.com/3"));
        mvc.perform(get("/urls/all?page=1&size=2").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].longUrl").value("https://example.com/1"));

        mvc.perform(get("/urls/all?page=0&size=1000").header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void openApiDocsAreServed() throws Exception {
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
    }
}
