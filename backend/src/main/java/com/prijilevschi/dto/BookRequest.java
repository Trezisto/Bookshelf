package com.prijilevschi.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public record BookRequest(
        @NotBlank String name,
        @NotBlank String authorName,
        String isbn,
        String description,
        String genre,
        String language,
        Integer year,
        @Min(1) Integer pages,
        Boolean read,
        LocalDate dateRead,
        Long shelfId,
        @Min(1) Integer positionNumber,
        @Min(1) Integer depthRow) {
}
