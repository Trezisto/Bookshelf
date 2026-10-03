package com.prijilevschi.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;
import java.util.List;

public record BookRequest(
        @NotBlank String name,
        @NotBlank String authorName,
        List<String> coAuthors,
        String isbn,
        String description,
        String genre,
        String language,
        String publisher,
        @Pattern(regexp = "^\\s*(https?://\\S+)?\\s*$", message = "must be an http(s) link") String url,
        @DecimalMin("0") @DecimalMax("5") Double rating,
        LocalDate publicationDate,
        @Min(1) Integer pages,
        Boolean read,
        LocalDate dateRead,
        Long shelfId,
        @Min(1) Integer positionNumber,
        @Min(1) Integer depthRow) {
}
