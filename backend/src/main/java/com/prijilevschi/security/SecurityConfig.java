package com.prijilevschi.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * Every {@code /api} endpoint needs the API token from {@code library.api-token} (env {@code LIBRARY_API_TOKEN}).
 * Starting without a token fails, unless {@code library.allow-anonymous=true} explicitly opts out
 * (for local development only: then anyone who can reach the port can change or delete the library).
 */
@Configuration
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);
    static final int MIN_TOKEN_LENGTH = 16;

    @Bean
    FilterRegistrationBean<ApiTokenFilter> apiTokenFilter(@Value("${library.api-token:}") String token,
                                                          @Value("${library.allow-anonymous:false}") boolean allowAnonymous) {
        String trimmed = token == null ? "" : token.strip();
        FilterRegistrationBean<ApiTokenFilter> registration = new FilterRegistrationBean<>();
        if (trimmed.isEmpty()) {
            if (!allowAnonymous) {
                throw new IllegalStateException("No API token configured. Set LIBRARY_API_TOKEN to a random secret "
                        + "of at least " + MIN_TOKEN_LENGTH + " characters (e.g. `openssl rand -hex 24`), "
                        + "or set LIBRARY_ALLOW_ANONYMOUS=true for local development only.");
            }
            log.warn("library.allow-anonymous=true: the API is open to anyone who can reach this server");
            registration.setEnabled(false);
            registration.setFilter(new ApiTokenFilter("unused"));
            return registration;
        }
        if (trimmed.length() < MIN_TOKEN_LENGTH) {
            throw new IllegalStateException("LIBRARY_API_TOKEN must be at least " + MIN_TOKEN_LENGTH + " characters");
        }
        registration.setFilter(new ApiTokenFilter(trimmed));
        registration.addUrlPatterns("/api/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        return registration;
    }
}
