package com.prijilevschi.service;

import com.prijilevschi.ai.LlmConfig;
import com.prijilevschi.ai.SummaryService;
import com.prijilevschi.dto.BookDTO;
import com.prijilevschi.dto.BookRequest;
import com.prijilevschi.dto.ReadRequest;
import com.prijilevschi.entity.BookEntity;
import com.prijilevschi.entity.BookPhotoEntity;
import com.prijilevschi.entity.ShelfEntity;
import com.prijilevschi.error.BadRequestException;
import com.prijilevschi.error.ConflictException;
import com.prijilevschi.error.NotFoundException;
import com.prijilevschi.error.SummaryUnavailableException;
import com.prijilevschi.repository.BookPhotoRepository;
import com.prijilevschi.repository.BookRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@Transactional
public class BookService {

    private static final Logger log = LoggerFactory.getLogger(BookService.class);
    private static final Set<String> COVER_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    private final BookRepository bookRepository;
    private final BookPhotoRepository bookPhotoRepository;
    private final AuthorService authorService;
    private final ShelfService shelfService;
    private final SummaryService summaryService;

    public BookService(BookRepository bookRepository, BookPhotoRepository bookPhotoRepository,
                       AuthorService authorService, ShelfService shelfService, SummaryService summaryService) {
        this.bookRepository = bookRepository;
        this.bookPhotoRepository = bookPhotoRepository;
        this.authorService = authorService;
        this.shelfService = shelfService;
        this.summaryService = summaryService;
    }

    /**
     * @param query matches title or author (substring, any case) or an exact ISBN
     */
    @Transactional(readOnly = true)
    public List<BookDTO> search(String query, Long shelfId, Boolean read, String language) {
        String pattern = null;
        String isbn = null;
        if (query != null && !query.isBlank()) {
            pattern = "%" + query.strip().toLowerCase(Locale.ROOT) + "%";
            isbn = Isbn.normalize(query);
        }
        String lang = language == null || language.isBlank() ? null : language.strip().toLowerCase(Locale.ROOT);
        return bookRepository.search(pattern, isbn, shelfId, read, lang).stream().map(Mapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public BookDTO get(Long id) {
        return Mapper.toDto(getEntity(id));
    }

    @Transactional(readOnly = true)
    public List<String> languages() {
        return bookRepository.findDistinctLanguages();
    }

    public BookDTO create(BookRequest request, LlmConfig llm) {
        BookEntity book = new BookEntity();
        apply(book, request, llm);
        return Mapper.toDto(bookRepository.save(book));
    }

    public BookDTO update(Long id, BookRequest request, LlmConfig llm) {
        BookEntity book = getEntity(id);
        apply(book, request, llm);
        return Mapper.toDto(book);
    }

    public void delete(Long id) {
        bookRepository.delete(getEntity(id));
    }

    public BookDTO setRead(Long id, ReadRequest request) {
        BookEntity book = getEntity(id);
        book.setRead(request.isRead());
        book.setDateRead(readDate(request.isRead(), request.dateRead()));
        return Mapper.toDto(book);
    }

    public void setCover(Long id, byte[] bytes, String contentType) {
        BookEntity book = getEntity(id);
        if (bytes == null || bytes.length == 0) {
            throw new BadRequestException("Cover image is empty");
        }
        if (contentType == null || !COVER_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new BadRequestException("Cover must be a JPEG, PNG or WebP image");
        }
        BookPhotoEntity photo = bookPhotoRepository.findById(id).orElseThrow();
        photo.setPhoto(bytes);
        photo.setPhotoContentType(contentType.toLowerCase(Locale.ROOT));
        book.setHasPhoto(true);
    }

    public void deleteCover(Long id) {
        BookEntity book = getEntity(id);
        BookPhotoEntity photo = bookPhotoRepository.findById(id).orElseThrow();
        photo.setPhoto(null);
        photo.setPhotoContentType(null);
        book.setHasPhoto(false);
    }

    @Transactional(readOnly = true)
    public BookPhotoEntity getCover(Long id) {
        return bookPhotoRepository.findById(id)
                .filter(photo -> photo.getPhoto() != null)
                .orElseThrow(() -> new NotFoundException("Book " + id + " has no cover"));
    }

    private BookEntity getEntity(Long id) {
        return bookRepository.findWithAuthorAndShelfById(id)
                .orElseThrow(() -> new NotFoundException("Book " + id + " not found"));
    }

    private void apply(BookEntity book, BookRequest request, LlmConfig llm) {
        String isbn = Isbn.requireValidOrNull(request.isbn());
        if (isbn != null) {
            bookRepository.findByIsbn(isbn)
                    .filter(other -> !other.getId().equals(book.getId()))
                    .ifPresent(other -> {
                        throw new ConflictException("ISBN " + isbn + " is already used by \"" + other.getName() + "\"");
                    });
        }

        book.setName(request.name().strip());
        book.setAuthor(authorService.findOrCreate(request.authorName()));
        book.setIsbn(isbn);
        book.setGenre(blankToNull(request.genre()));
        book.setLanguage(blankToNull(request.language()));
        book.setPublisher(blankToNull(request.publisher()));
        book.setUrl(blankToNull(request.url()));
        book.setRating(request.rating());
        book.setPublicationDate(request.publicationDate());
        book.setCoAuthors(cleanCoAuthors(request.coAuthors(), book.getAuthor().getName()));
        book.setPages(request.pages());
        boolean read = Boolean.TRUE.equals(request.read());
        book.setRead(read);
        book.setDateRead(readDate(read, request.dateRead()));
        place(book, request);

        String description = blankToNull(request.description());
        if (description == null && llm != null && llm.hasApiKey()) {
            description = tryGenerateSummary(book, llm);
        }
        book.setDescription(description);
    }

    private void place(BookEntity book, BookRequest request) {
        int depthRow = request.depthRow() == null ? 1 : request.depthRow();
        if (request.shelfId() == null) {
            book.setShelf(null);
            book.setPositionNumber(null);
            book.setDepthRow(depthRow);
            return;
        }
        if (request.positionNumber() == null) {
            throw new BadRequestException("positionNumber is required when a shelf is chosen");
        }
        ShelfEntity shelf = shelfService.getEntity(request.shelfId());
        bookRepository.findFirstByShelfIdAndDepthRowAndPositionNumber(shelf.getId(), depthRow, request.positionNumber())
                .filter(other -> !other.getId().equals(book.getId()))
                .ifPresent(other -> {
                    throw new ConflictException("Position " + request.positionNumber() + " in depth row " + depthRow
                            + " is already taken by \"" + other.getName() + "\"");
                });
        book.setShelf(shelf);
        book.setPositionNumber(request.positionNumber());
        book.setDepthRow(depthRow);
    }

    private String tryGenerateSummary(BookEntity book, LlmConfig llm) {
        try {
            return summaryService.summarize(book.getName(), book.getAuthor().getName(), book.getIsbn(),
                    book.getLanguage(), llm);
        } catch (SummaryUnavailableException e) {
            log.info("Saving \"{}\" without a summary: {}", book.getName(), e.getMessage());
            return null;
        }
    }

    /** Trims, drops blanks, duplicates (any case) and the main author; keeps the entered order. */
    private static List<String> cleanCoAuthors(List<String> names, String mainAuthor) {
        if (names == null) {
            return List.of();
        }
        Set<String> seen = new HashSet<>();
        seen.add(mainAuthor.strip().toLowerCase(Locale.ROOT));
        List<String> cleaned = new ArrayList<>();
        for (String name : names) {
            String value = blankToNull(name);
            if (value != null && seen.add(value.toLowerCase(Locale.ROOT))) {
                cleaned.add(value);
            }
        }
        return cleaned;
    }

    private static LocalDate readDate(boolean read, LocalDate date) {
        if (!read) {
            return null;
        }
        return date == null ? LocalDate.now() : date;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
