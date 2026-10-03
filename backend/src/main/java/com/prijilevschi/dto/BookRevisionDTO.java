package com.prijilevschi.dto;

import java.time.Instant;
import java.time.LocalDate;

/** The state of a book at one Envers revision. {@code type} is ADD, MOD or DEL (a deleted book keeps only its id). */
public record BookRevisionDTO(
        long revision,
        Instant timestamp,
        String type,
        String name,
        String isbn,
        String publisher,
        LocalDate publicationDate,
        Double rating,
        Boolean read) {
}
