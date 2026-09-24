package com.yato.urlShortenerb.util;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Groups a raw User-Agent header into a coarse device type for analytics.
 * <p>
 * Heuristic, not exhaustive: iPads on iPadOS 13+ send a desktop Safari
 * User-Agent by default and are counted as Desktop.
 */
public final class DeviceClassifier {

    public static final String MOBILE = "Mobile";
    public static final String TABLET = "Tablet";
    public static final String DESKTOP = "Desktop";
    public static final String BOT = "Bot";
    public static final String OTHER = "Other";
    public static final String UNKNOWN = "Unknown";

    private static final Pattern BOT_PATTERN = Pattern.compile(
            "bot|crawl|spider|slurp|preview|facebookexternalhit|headless|lighthouse"
                    + "|curl|wget|python-requests|httpclient|okhttp|go-http-client|java/|postman");
    private static final Pattern TABLET_PATTERN = Pattern.compile("ipad|tablet|kindle|silk|playbook");
    private static final Pattern MOBILE_PATTERN = Pattern.compile(
            "mobi|iphone|ipod|android|blackberry|bb10|opera mini|windows phone|iemobile");

    private DeviceClassifier() {
    }

    public static String classify(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) return UNKNOWN;
        String ua = userAgent.toLowerCase(Locale.ROOT);

        if (BOT_PATTERN.matcher(ua).find()) return BOT;
        if (TABLET_PATTERN.matcher(ua).find()) return TABLET;
        // Android phones include "Mobile"; Android tablets do not
        if (ua.contains("android") && !ua.contains("mobile")) return TABLET;
        if (MOBILE_PATTERN.matcher(ua).find()) return MOBILE;
        // Every mainstream browser sends a "Mozilla/5.0 ..." (or legacy "Opera/...") User-Agent
        if (ua.startsWith("mozilla/") || ua.startsWith("opera/")) return DESKTOP;
        return OTHER;
    }
}
