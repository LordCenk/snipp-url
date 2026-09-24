package com.yato.urlShortenerb.util;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;

public final class UrlValidator {
    private static final Set<String> ALLOWED_SCHEMES = Set.of("http", "https");
    private static final int MAX_LENGTH = 2048;

    private UrlValidator() {
    }

    /**
     * Only absolute http(s) URLs with a host are allowed, so short links can't
     * be used to serve javascript:, data:, file: or other dangerous schemes.
     */
    public static boolean isValidHttpUrl(String value) {
        if (value == null || value.isBlank() || value.length() > MAX_LENGTH) return false;
        try {
            URI uri = new URI(value.trim());
            String scheme = uri.getScheme();
            return scheme != null
                    && ALLOWED_SCHEMES.contains(scheme.toLowerCase())
                    && uri.getHost() != null
                    && !uri.getHost().isBlank();
        } catch (URISyntaxException e) {
            return false;
        }
    }
}
