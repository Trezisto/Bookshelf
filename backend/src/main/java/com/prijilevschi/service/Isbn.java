package com.prijilevschi.service;

import com.prijilevschi.error.BadRequestException;

/** ISBN-10 / ISBN-13 normalisation and checksum validation. */
public final class Isbn {
    private Isbn() {
    }

    /** Strips spaces and hyphens and upper-cases a trailing x. Returns null for blank input. */
    public static String normalize(String raw) {
        if (raw == null) {
            return null;
        }
        String isbn = raw.replaceAll("[\\s-]", "").toUpperCase();
        return isbn.isEmpty() ? null : isbn;
    }

    public static boolean isValid(String isbn) {
        if (isbn == null) {
            return false;
        }
        if (isbn.matches("\\d{9}[\\dX]")) {
            int sum = 0;
            for (int i = 0; i < 10; i++) {
                char c = isbn.charAt(i);
                int digit = c == 'X' ? 10 : c - '0';
                sum += digit * (10 - i);
            }
            return sum % 11 == 0;
        }
        if (isbn.matches("\\d{13}")) {
            int sum = 0;
            for (int i = 0; i < 13; i++) {
                int digit = isbn.charAt(i) - '0';
                sum += i % 2 == 0 ? digit : digit * 3;
            }
            return sum % 10 == 0;
        }
        return false;
    }

    /** Normalises and validates; null stays null, an invalid value is rejected with 400. */
    public static String requireValidOrNull(String raw) {
        String isbn = normalize(raw);
        if (isbn != null && !isValid(isbn)) {
            throw new BadRequestException("Invalid ISBN: " + raw);
        }
        return isbn;
    }
}
