package com.prijilevschi.dto;

import jakarta.validation.constraints.NotBlank;

public record SummaryRequest(@NotBlank String title, String author, String isbn, String language) {
}
