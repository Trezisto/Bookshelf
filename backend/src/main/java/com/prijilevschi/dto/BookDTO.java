package com.prijilevschi.dto;

import java.time.LocalDate;

public record BookDTO(
        Long id,
        String name,
        String isbn,
        String description,
        String genre,
        String language,
        Integer year,
        Integer pages,
        boolean read,
        LocalDate dateRead,
        AuthorDTO author,
        ShelfDTO shelf,
        Integer positionNumber,
        int depthRow,
        boolean hasCover) {
}
