package com.yato.urlShortenerb.util;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DeviceClassifierTests {

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            // Desktop browsers
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/129.0.0.0 Safari/537.36 | Desktop",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 14_5) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.5 Safari/605.1.15 | Desktop",
            "Mozilla/5.0 (X11; Linux x86_64; rv:130.0) Gecko/20100101 Firefox/130.0 | Desktop",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/129.0.0.0 Safari/537.36 Edg/129.0.0.0 | Desktop",
            // Phones
            "Mozilla/5.0 (iPhone; CPU iPhone OS 17_5 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.5 Mobile/15E148 Safari/604.1 | Mobile",
            "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/129.0.0.0 Mobile Safari/537.36 | Mobile",
            "Mozilla/5.0 (Linux; Android 13; SM-S911B) AppleWebKit/537.36 (KHTML, like Gecko) SamsungBrowser/25.0 Chrome/121.0.0.0 Mobile Safari/537.36 | Mobile",
            "Opera/9.80 (J2ME/MIDP; Opera Mini/9.80 (S60; SymbOS; Opera Mobi/23.348; U; en) Presto/2.5.25 Version/10.54 | Mobile",
            // Tablets
            "Mozilla/5.0 (iPad; CPU OS 12_2 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/12.1 Mobile/15E148 Safari/604.1 | Tablet",
            "Mozilla/5.0 (Linux; Android 14; SM-X710) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/129.0.0.0 Safari/537.36 | Tablet",
            "Mozilla/5.0 (Linux; U; Android 4.0.3; en-us; KFTT Build/IML74K) AppleWebKit/537.36 (KHTML, like Gecko) Silk/3.68 like Chrome/39.0.2171.93 Safari/537.36 | Tablet",
            // Bots, link previews and scripts
            "Mozilla/5.0 (compatible; Googlebot/2.1; +http://www.google.com/bot.html) | Bot",
            "Mozilla/5.0 (Linux; Android 6.0.1; Nexus 5X Build/MMB29P) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/129.0.0.0 Mobile Safari/537.36 (compatible; Googlebot/2.1; +http://www.google.com/bot.html) | Bot",
            "facebookexternalhit/1.1 (+http://www.facebook.com/externalhit_uatext.php) | Bot",
            "Twitterbot/1.0 | Bot",
            "Slackbot-LinkExpanding 1.0 (+https://api.slack.com/robots) | Bot",
            "curl/8.5.0 | Bot",
            "python-requests/2.32.3 | Bot",
            // Unrecognised non-browser clients
            "MyApp/1.2 (custom client) | Other",
    })
    void classifiesUserAgents(String userAgent, String expected) {
        assertEquals(expected, DeviceClassifier.classify(userAgent));
    }

    @ParameterizedTest
    @NullAndEmptySource
    void missingUserAgentIsUnknown(String userAgent) {
        assertEquals("Unknown", DeviceClassifier.classify(userAgent));
    }
}
