package com.prijilevschi.controller;

import com.prijilevschi.dto.BookLookupDTO;
import com.prijilevschi.error.BadRequestException;
import com.prijilevschi.error.NotFoundException;
import com.prijilevschi.lookup.BookLookupService;
import com.prijilevschi.service.Isbn;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Finds book details on the web, for prefilling the add-book form after scanning or typing an ISBN. */
@RestController
@RequestMapping("/api/lookup")
public class LookupController {
    private final BookLookupService lookupService;

    public LookupController(BookLookupService lookupService) {
        this.lookupService = lookupService;
    }

    /** Either {@code isbn}, or {@code title} (optionally with {@code author}). */
    @GetMapping
    public BookLookupDTO lookup(@RequestParam(required = false) String isbn,
                                @RequestParam(required = false) String title,
                                @RequestParam(required = false) String author) {
        String normalized = Isbn.requireValidOrNull(isbn);
        if (normalized != null) {
            return lookupService.byIsbn(normalized)
                    .orElseThrow(() -> new NotFoundException("Nothing found for ISBN " + normalized));
        }
        if (title == null || title.isBlank()) {
            throw new BadRequestException("Provide an isbn, or a title (with an optional author)");
        }
        return lookupService.byTitleAuthor(title.strip(), author)
                .orElseThrow(() -> new NotFoundException("Nothing found for \"" + title.strip() + "\""));
    }
}
