package com.prijilevschi.dto;

import com.prijilevschi.entity.ShelfOrientation;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ShelfRequest(
        @NotBlank String location,
        @NotNull @Min(1) Integer rowNum,
        @NotNull ShelfOrientation orientation) {
}
