package com.prijilevschi.dto;

import com.prijilevschi.entity.ShelfOrientation;

public record ShelfDTO(Long id, String location, int rowNum, ShelfOrientation orientation) {
}
