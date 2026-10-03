package com.prijilevschi.lookup;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Finds the Goodreads page of a book without ever requesting goodreads.com: the id comes from Open Library,
 * otherwise from a {@code site:goodreads.com} web search whose result links are read.
 */
@Component
public class GoodreadsUrlResolver {

    private static final Logger log = LoggerFactory.getLogger(GoodreadsUrlResolver.class);
    private static final String BOOK_URL = "https://www.goodreads.com/book/show/";
    /** Matches plain and percent-encoded links: goodreads.com/book/show/123 or goodreads.com%2Fbook%2Fshow%2F123 */
    private static final Pattern RESULT = Pattern.compile("goodreads\\.com(?:/|%2F)book(?:/|%2F)show(?:/|%2F)(\\d+)");

    private final RestClient search;

    GoodreadsUrlResolver(LookupProperties properties) {
        this.search = HttpSources.client(properties.webSearchUrl(), properties);
    }

    static String urlOf(String goodreadsId) {
        return BOOK_URL + goodreadsId;
    }

    /** @param query free text such as an ISBN or "title author"; returns null when nothing is found */
    String searchWeb(String query) {
        String html;
        try {
            html = search.get()
                    .uri(uri -> uri.queryParam("q", "site:goodreads.com/book/show " + query).build())
                    .retrieve().body(String.class);
        } catch (RestClientException e) {
            log.info("Web search for a Goodreads link failed: {}", e.getMessage());
            return null;
        }
        if (html == null) {
            return null;
        }
        Matcher match = RESULT.matcher(URLDecoder.decode(html.replaceAll("%(?![0-9A-Fa-f]{2})", "%25"), StandardCharsets.UTF_8));
        return match.find() ? urlOf(match.group(1)) : null;
    }
}
