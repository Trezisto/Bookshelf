package com.prijilevschi.lookup;

import com.prijilevschi.dto.BookLookupDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Google Books volumes search (no key needed for light use). */
@Component
class GoogleBooksClient {

    private static final Logger log = LoggerFactory.getLogger(GoogleBooksClient.class);

    private final RestClient client;
    private final String apiKey;

    GoogleBooksClient(LookupProperties properties) {
        this.apiKey = properties.googleBooksKey() == null || properties.googleBooksKey().isBlank()
                ? null : properties.googleBooksKey().strip();
        this.client = HttpSources.client(properties.googleBooksUrl(), properties);
    }

    Optional<BookLookupDTO> byIsbn(String isbn) {
        return first("isbn:" + isbn, isbn);
    }

    Optional<BookLookupDTO> byTitleAuthor(String title, String author) {
        String query = "intitle:" + title.strip() + (author == null || author.isBlank() ? "" : " inauthor:" + author.strip());
        return first(query, null);
    }

    private Optional<BookLookupDTO> first(String query, String isbn) {
        JsonNode result;
        try {
            result = client.get()
                    .uri(uri -> uri.path("/volumes").queryParam("q", query).queryParam("maxResults", 1)
                            .queryParamIfPresent("key", Optional.ofNullable(apiKey)).build())
                    .retrieve().body(JsonNode.class);
        } catch (RestClientException e) {
            log.info("Google Books request failed: {}", e.getMessage());
            return Optional.empty();
        }
        JsonNode info = result == null ? null : result.path("items").path(0).path("volumeInfo");
        if (info == null || info.isMissingNode() || info.isNull()) {
            return Optional.empty();
        }
        List<String> authors = new ArrayList<>();
        info.path("authors").forEach(name -> authors.add(name.asString("")));
        authors.removeIf(String::isBlank);
        String title = blankToNull(info.path("title").asString(""));
        String subtitle = blankToNull(info.path("subtitle").asString(""));
        String cover = blankToNull(info.path("imageLinks").path("thumbnail").asString(""));
        String foundIsbn = isbn;
        if (foundIsbn == null) {
            for (JsonNode id : info.path("industryIdentifiers")) {
                if ("ISBN_13".equals(id.path("type").asString(""))) {
                    foundIsbn = id.path("identifier").asString("");
                    break;
                }
            }
        }
        return Optional.of(new BookLookupDTO(
                foundIsbn,
                title == null ? null : subtitle == null ? title : title + ": " + subtitle,
                authors.isEmpty() ? null : authors.getFirst(),
                authors.size() > 1 ? List.copyOf(authors.subList(1, authors.size())) : List.of(),
                blankToNull(info.path("publisher").asString("")),
                Dates.parse(info.path("publishedDate").asString("")),
                Languages.nameOf(info.path("language").asString("")),
                info.path("pageCount").asInt(0) > 0 ? info.path("pageCount").asInt() : null,
                blankToNull(info.path("categories").path(0).asString("")),
                blankToNull(info.path("description").asString("")),
                cover == null ? null : cover.replaceFirst("^http://", "https://"),
                null));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
