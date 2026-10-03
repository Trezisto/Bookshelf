package com.prijilevschi.lookup;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Endpoints of the public book data sources (overridable for tests) and the HTTP timeouts.
 * {@code googleBooksKey} is optional; without it Google Books applies a small shared anonymous quota.
 */
@ConfigurationProperties("library.lookup")
public record LookupProperties(
        String openLibraryUrl,
        String googleBooksUrl,
        String googleBooksKey,
        String coversUrl,
        String webSearchUrl,
        Duration connectTimeout,
        Duration readTimeout) {

    public LookupProperties {
        openLibraryUrl = orDefault(openLibraryUrl, "https://openlibrary.org");
        googleBooksUrl = orDefault(googleBooksUrl, "https://www.googleapis.com/books/v1");
        coversUrl = orDefault(coversUrl, "https://covers.openlibrary.org");
        webSearchUrl = orDefault(webSearchUrl, "https://html.duckduckgo.com/html/");
        connectTimeout = connectTimeout == null ? Duration.ofSeconds(3) : connectTimeout;
        readTimeout = readTimeout == null ? Duration.ofSeconds(6) : readTimeout;
    }

    private static String orDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.strip();
    }
}
