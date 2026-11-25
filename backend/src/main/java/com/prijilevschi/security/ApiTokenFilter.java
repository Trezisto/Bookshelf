package com.prijilevschi.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Lets an {@code /api} request through only with {@code Authorization: Bearer <token>}.
 * The token is compared in constant time; a missing or wrong token gets a 401 problem response.
 */
public class ApiTokenFilter extends OncePerRequestFilter {

    static final String PREFIX = "Bearer ";

    private final byte[] token;

    public ApiTokenFilter(String token) {
        this.token = token.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (matches(request.getHeader(HttpHeaders.AUTHORIZATION))) {
            chain.doFilter(request, response);
            return;
        }
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.getWriter().write("""
                {"type":"about:blank","title":"Unauthorized","status":401,\
                "detail":"Missing or wrong API token. Enter the server's token in the app's Settings."}""");
    }

    private boolean matches(String header) {
        if (header == null || !header.regionMatches(true, 0, PREFIX, 0, PREFIX.length())) {
            return false;
        }
        byte[] given = header.substring(PREFIX.length()).strip().getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(given, token);
    }
}
