package com.prijilevschi.ai;

import com.prijilevschi.dto.BookRequest;
import com.prijilevschi.error.SummaryUnavailableException;
import com.prijilevschi.service.Isbn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Fills in a missing description with an LLM summary before a book is saved.
 * Runs outside any database transaction: an LLM call can take tens of seconds, and SQLite has a single
 * connection, so holding it during the call would stall every other request.
 */
@Component
public class BookSummaries {

    private static final Logger log = LoggerFactory.getLogger(BookSummaries.class);

    private final SummaryService summaryService;

    public BookSummaries(SummaryService summaryService) {
        this.summaryService = summaryService;
    }

    /** The request with a generated description when it has none and an API key is present; failures are logged. */
    public BookRequest withSummary(BookRequest request, LlmConfig llm) {
        if (llm == null || !llm.hasApiKey() || !isBlank(request.description())
                || isBlank(request.name()) || isBlank(request.authorName())) {
            return request;
        }
        String isbn = Isbn.normalize(request.isbn());
        try {
            String summary = summaryService.summarize(request.name().strip(), request.authorName().strip(),
                    isbn != null && Isbn.isValid(isbn) ? isbn : null,
                    isBlank(request.language()) ? null : request.language().strip(), llm);
            return request.withDescription(summary);
        } catch (SummaryUnavailableException e) {
            log.info("Saving \"{}\" without a summary: {}", request.name(), e.getMessage());
            return request;
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
