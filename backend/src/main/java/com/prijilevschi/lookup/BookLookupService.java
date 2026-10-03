package com.prijilevschi.lookup;

import com.prijilevschi.dto.BookLookupDTO;
import com.prijilevschi.dto.BookRequest;
import com.prijilevschi.service.Isbn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Collects what the web knows about a book from Open Library (primary) and Google Books (fills the gaps),
 * plus the book's Goodreads link. Every source failure is tolerated: the result is then simply emptier.
 */
@Service
@EnableConfigurationProperties(LookupProperties.class)
public class BookLookupService {

    private static final Logger log = LoggerFactory.getLogger(BookLookupService.class);

    private final OpenLibraryClient openLibrary;
    private final GoogleBooksClient googleBooks;
    private final GoodreadsUrlResolver goodreads;

    BookLookupService(OpenLibraryClient openLibrary, GoogleBooksClient googleBooks, GoodreadsUrlResolver goodreads) {
        this.openLibrary = openLibrary;
        this.googleBooks = googleBooks;
        this.goodreads = goodreads;
    }

    /** @param isbn normalised and valid */
    public Optional<BookLookupDTO> byIsbn(String isbn) {
        Optional<OpenLibraryClient.Found> open = openLibrary.byIsbn(isbn);
        Optional<BookLookupDTO> google = googleBooks.byIsbn(isbn);
        if (open.isEmpty() && google.isEmpty()) {
            return Optional.empty();
        }
        BookLookupDTO merged = merge(open.map(OpenLibraryClient.Found::book).orElse(null), google.orElse(null));
        String url = open.map(OpenLibraryClient.Found::goodreadsId).map(GoodreadsUrlResolver::urlOf).orElse(null);
        if (url == null) {
            url = goodreads.searchWeb(isbn);
        }
        if (url == null && merged.title() != null) {
            url = goodreads.searchWeb(merged.title() + (merged.author() == null ? "" : " " + merged.author()));
        }
        return Optional.of(withUrl(merged, url));
    }

    public Optional<BookLookupDTO> byTitleAuthor(String title, String author) {
        Optional<OpenLibraryClient.Found> open = openLibrary.search(title, author);
        Optional<BookLookupDTO> google = googleBooks.byTitleAuthor(title, author);
        if (open.isEmpty() && google.isEmpty()) {
            return Optional.empty();
        }
        BookLookupDTO merged = merge(open.map(OpenLibraryClient.Found::book).orElse(null), google.orElse(null));
        String url = open.map(OpenLibraryClient.Found::goodreadsId).map(GoodreadsUrlResolver::urlOf).orElse(null);
        if (url == null) {
            url = goodreads.searchWeb(searchText(title, author));
        }
        return Optional.of(withUrl(merged, url));
    }

    /** Goodreads link only: by ISBN when there is one, otherwise by title and author. */
    public Optional<String> goodreadsUrl(String isbn, String title, String author) {
        if (isbn != null) {
            String id = openLibrary.goodreadsIdByIsbn(isbn);
            if (id != null) {
                return Optional.of(GoodreadsUrlResolver.urlOf(id));
            }
            Optional<String> found = Optional.ofNullable(goodreads.searchWeb(isbn));
            if (found.isPresent()) {
                return found;
            }
        }
        if (title == null || title.isBlank()) {
            return Optional.empty();
        }
        Optional<String> id = openLibrary.search(title, author).map(OpenLibraryClient.Found::goodreadsId);
        if (id.isPresent()) {
            return id.map(GoodreadsUrlResolver::urlOf);
        }
        return Optional.ofNullable(goodreads.searchWeb(searchText(title, author)));
    }

    /**
     * A request for a new book without a link gets its Goodreads page (best effort, failures are logged).
     * Runs before the save so no database connection is held while the web is queried.
     */
    public BookRequest withGoodreadsUrl(BookRequest request) {
        if (request.url() != null && !request.url().isBlank()) {
            return request;
        }
        String isbn = Isbn.normalize(request.isbn());
        try {
            Optional<String> url = goodreadsUrl(isbn != null && Isbn.isValid(isbn) ? isbn : null,
                    request.name(), request.authorName());
            return url.map(found -> new BookRequest(request.name(), request.authorName(), request.coAuthors(),
                    request.isbn(), request.description(), request.genre(), request.language(), request.publisher(),
                    found, request.rating(), request.publicationDate(), request.pages(), request.read(),
                    request.dateRead(), request.shelfId(), request.positionNumber(), request.depthRow()))
                    .orElse(request);
        } catch (RuntimeException e) {
            log.info("Saving \"{}\" without a Goodreads link: {}", request.name(), e.getMessage());
            return request;
        }
    }

    private static String searchText(String title, String author) {
        return title.strip() + (author == null || author.isBlank() ? "" : " " + author.strip());
    }

    private static BookLookupDTO withUrl(BookLookupDTO book, String url) {
        return new BookLookupDTO(book.isbn(), book.title(), book.author(), book.coAuthors(), book.publisher(),
                book.publicationDate(), book.language(), book.pages(), book.genre(), book.description(),
                book.coverUrl(), url);
    }

    /** Every field of {@code primary} wins; {@code fallback} only fills what is missing. */
    private static BookLookupDTO merge(BookLookupDTO primary, BookLookupDTO fallback) {
        if (primary == null) {
            return fallback;
        }
        if (fallback == null) {
            return primary;
        }
        boolean primaryHasAuthor = primary.author() != null;
        List<String> coAuthors = primaryHasAuthor ? primary.coAuthors() : fallback.coAuthors();
        return new BookLookupDTO(
                first(primary.isbn(), fallback.isbn()),
                first(primary.title(), fallback.title()),
                first(primary.author(), fallback.author()),
                coAuthors == null ? List.of() : coAuthors,
                first(primary.publisher(), fallback.publisher()),
                first(primary.publicationDate(), fallback.publicationDate()),
                first(primary.language(), fallback.language()),
                first(primary.pages(), fallback.pages()),
                first(primary.genre(), fallback.genre()),
                first(primary.description(), fallback.description()),
                first(primary.coverUrl(), fallback.coverUrl()),
                null);
    }

    private static <T> T first(T a, T b) {
        return a != null ? a : b;
    }
}
