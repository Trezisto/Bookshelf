package com.prijilevschi.service;

import com.prijilevschi.dto.AuthorDTO;
import com.prijilevschi.dto.BookDTO;
import com.prijilevschi.dto.ShelfDTO;
import com.prijilevschi.entity.AuthorEntity;
import com.prijilevschi.entity.BookEntity;
import com.prijilevschi.entity.ShelfEntity;

final class Mapper {
    private Mapper() {
    }

    static AuthorDTO toDto(AuthorEntity author) {
        return new AuthorDTO(author.getId(), author.getName());
    }

    static ShelfDTO toDto(ShelfEntity shelf) {
        return shelf == null ? null
                : new ShelfDTO(shelf.getId(), shelf.getLocation(), shelf.getRowNum(), shelf.getOrientation());
    }

    static BookDTO toDto(BookEntity book) {
        return new BookDTO(
                book.getId(),
                book.getName(),
                book.getIsbn(),
                book.getDescription(),
                book.getGenre(),
                book.getLanguage(),
                book.getPublishYear(),
                book.getPages(),
                book.isRead(),
                book.getDateRead(),
                toDto(book.getAuthor()),
                toDto(book.getShelf()),
                book.getPositionNumber(),
                book.getDepthRow(),
                book.isHasPhoto());
    }
}
