package com.yato.urlShortenerb.util;

public final class LogMasker {

    private LogMasker() {
    }

    /** Masks an email for logging: "alice@example.com" -> "a***@example.com". */
    public static String maskEmail(String email) {
        if (email == null) return "null";
        int at = email.indexOf('@');
        if (at <= 0) return "***";
        return email.charAt(0) + "***" + email.substring(at);
    }
}
