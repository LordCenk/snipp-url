package com.yato.urlShortenerb.config;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class JWTUtilsTests {

    private static JWTUtils withSecret(String secret) {
        JWTUtils utils = new JWTUtils();
        ReflectionTestUtils.setField(utils, "jwtSecret", secret);
        ReflectionTestUtils.setField(utils, "jwtExpirationMs", 60_000L);
        return utils;
    }

    @Test
    void rejectsMissingOrShortSecretAtStartup() {
        assertThrows(IllegalStateException.class, () -> withSecret(null).init());
        assertThrows(IllegalStateException.class, () -> withSecret("too-short").init());
    }

    @Test
    void issuesAndValidatesTokensWithValidSecret() {
        JWTUtils utils = withSecret("0123456789abcdef0123456789abcdef");
        utils.init();
        String token = utils.generateToken("a@test.com");
        assertTrue(utils.validateToken(token));
        assertEquals("a@test.com", utils.getEmailFromToken(token));
        assertFalse(utils.validateToken(token + "x"));
    }
}
