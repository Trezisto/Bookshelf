package com.prijilevschi.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record BookDTO(
        Long id,
        String name,
        String isbn,
        String description,
        String genre,
        String language,
        String publisher,
        String url,
        Double rating,
        LocalDate publicationDate,
        Integer pages,
        boolean read,
        LocalDate dateRead,
        AuthorDTO author,
        List<String> coAuthors,
        ShelfDTO shelf,
        Integer positionNumber,
        int depthRow,
        boolean hasCover,
        Instant createdAt,
        Instant modifiedAt) {
}
