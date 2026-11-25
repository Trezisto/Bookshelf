package com.prijilevschi.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;
import java.util.Locale;

/**
 * LLM defaults and the base URLs the app may ask the server to call. The base URL arrives in a request header,
 * so without this list anyone who can reach the API could make the server send requests to any address.
 * The default base URL is always allowed.
 */
@ConfigurationProperties("library.llm")
public record LlmProperties(String defaultBaseUrl, String defaultModel, Duration timeout, List<String> allowedBaseUrls) {

    public LlmProperties {
        allowedBaseUrls = allowedBaseUrls == null ? List.of() : List.copyOf(allowedBaseUrls);
    }

    public boolean isAllowed(String baseUrl) {
        String wanted = normalize(baseUrl);
        return wanted.equals(normalize(defaultBaseUrl))
                || allowedBaseUrls.stream().map(LlmProperties::normalize).anyMatch(wanted::equals);
    }

    /** Case-insensitive, without surrounding blanks or trailing slashes. */
    static String normalize(String url) {
        if (url == null) {
            return "";
        }
        String value = url.strip().toLowerCase(Locale.ROOT);
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }
}
