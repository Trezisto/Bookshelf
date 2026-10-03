package com.prijilevschi.dto;

import java.time.LocalDate;
import java.util.List;

/** Book data found on the web; any field may be null (or empty for {@code coAuthors}). */
public record BookLookupDTO(
        String isbn,
        String title,
        String author,
        List<String> coAuthors,
        String publisher,
        LocalDate publicationDate,
        String language,
        Integer pages,
        String genre,
        String description,
        String coverUrl,
        String url) {
}
