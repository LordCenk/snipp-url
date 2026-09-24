package com.yato.urlShortenerb;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
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
        mvc.perform(get("/s/" + code)).andExpect(status().isBadRequest());
    }

    @Test
    void openApiDocsAreServed() throws Exception {
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
    }
}
