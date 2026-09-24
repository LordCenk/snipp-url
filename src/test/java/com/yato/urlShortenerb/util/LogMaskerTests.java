package com.yato.urlShortenerb.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LogMaskerTests {

    @Test
    void masksEmails() {
        assertEquals("a***@example.com", LogMasker.maskEmail("alice@example.com"));
        assertEquals("***", LogMasker.maskEmail("no-at-sign"));
        assertEquals("null", LogMasker.maskEmail(null));
    }
}
