package com.prijilevschi.lookup;

import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/** Builds the HTTP client shared by the lookup sources: short timeouts and an identifying User-Agent. */
final class HttpSources {
    private HttpSources() {
    }

    static final String USER_AGENT = "Mozilla/5.0 (compatible; Bookshelf/1.0; personal home library)";

    static RestClient client(String baseUrl, LookupProperties properties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(properties.connectTimeout());
        factory.setReadTimeout(properties.readTimeout());
        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .defaultHeader("User-Agent", USER_AGENT)
                .build();
    }
}
