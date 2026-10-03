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

/** Open Library: edition by ISBN (with authors and work) and title/author search. */
@Component
class OpenLibraryClient {

    private static final Logger log = LoggerFactory.getLogger(OpenLibraryClient.class);
    private static final int MAX_AUTHORS = 6;

    private final RestClient client;
    private final LookupProperties properties;

    OpenLibraryClient(LookupProperties properties) {
        this.properties = properties;
        this.client = HttpSources.client(properties.openLibraryUrl(), properties);
    }

    /** Result of a lookup plus the Goodreads book id Open Library knows for it. */
    record Found(BookLookupDTO book, String goodreadsId) {
    }

    /** One cheap request: the Goodreads book id of the edition with this ISBN, or null. */
    String goodreadsIdByIsbn(String isbn) {
        JsonNode edition = get("/isbn/{isbn}.json", isbn);
        return edition == null ? null
                : blankToNull(edition.path("identifiers").path("goodreads").path(0).asString(""));
    }

    Optional<Found> byIsbn(String isbn) {
        JsonNode edition = get("/isbn/{isbn}.json", isbn);
        if (edition == null) {
            return Optional.empty();
        }
        List<String> authors = new ArrayList<>();
        int count = 0;
        for (JsonNode ref : edition.path("authors")) {
            if (count++ >= MAX_AUTHORS) {
                break;
            }
            JsonNode author = get(ref.path("key").asString("") + ".json");
            String name = author == null ? null : blankToNull(author.path("name").asString(""));
            if (name != null) {
                authors.add(name);
            }
        }
        JsonNode work = null;
        String workKey = edition.path("works").path(0).path("key").asString("");
        if (!workKey.isEmpty()) {
            work = get(workKey + ".json");
        }
        String description = description(edition.path("description"));
        if (description == null && work != null) {
            description = description(work.path("description"));
        }
        JsonNode subjects = edition.path("subjects").isArray() && !edition.path("subjects").isEmpty()
                ? edition.path("subjects") : work == null ? edition.path("subjects") : work.path("subjects");

        String title = blankToNull(edition.path("title").asString(""));
        String subtitle = blankToNull(edition.path("subtitle").asString(""));
        boolean hasCover = edition.path("covers").isArray() && !edition.path("covers").isEmpty();
        BookLookupDTO book = new BookLookupDTO(
                isbn,
                title == null ? null : subtitle == null ? title : title + ": " + subtitle,
                authors.isEmpty() ? null : authors.getFirst(),
                authors.size() > 1 ? List.copyOf(authors.subList(1, authors.size())) : List.of(),
                blankToNull(edition.path("publishers").path(0).asString("")),
                Dates.parse(edition.path("publish_date").asString("")),
                Languages.nameOf(edition.path("languages").path(0).path("key").asString("")),
                edition.path("number_of_pages").isNumber() ? edition.path("number_of_pages").asInt() : null,
                blankToNull(subjects.path(0).asString("")),
                description,
                hasCover ? properties.coversUrl() + "/b/isbn/" + isbn + "-L.jpg" : null,
                null);
        return Optional.of(new Found(book, blankToNull(edition.path("identifiers").path("goodreads").path(0).asString(""))));
    }

    Optional<Found> search(String title, String author) {
        JsonNode result;
        try {
            result = client.get()
                    .uri(uri -> uri.path("/search.json")
                            .queryParam("title", title)
                            .queryParamIfPresent("author", Optional.ofNullable(blankToNull(author)))
                            .queryParam("limit", 1)
                            .queryParam("fields", "title,author_name,first_publish_year,publisher,language,"
                                    + "number_of_pages_median,isbn,subject,cover_i,id_goodreads")
                            .build())
                    .retrieve().body(JsonNode.class);
        } catch (RestClientException e) {
            log.info("Open Library search failed: {}", e.getMessage());
            return Optional.empty();
        }
        JsonNode doc = result == null ? null : result.path("docs").path(0);
        if (doc == null || doc.isMissingNode() || doc.isNull()) {
            return Optional.empty();
        }
        List<String> authors = new ArrayList<>();
        doc.path("author_name").forEach(name -> authors.add(name.asString("")));
        authors.removeIf(String::isBlank);
        String isbn = null;
        for (JsonNode candidate : doc.path("isbn")) {
            String value = candidate.asString("");
            if (value.length() == 13 && com.prijilevschi.service.Isbn.isValid(value)) {
                isbn = value;
                break;
            }
        }
        int coverId = doc.path("cover_i").asInt(0);
        BookLookupDTO book = new BookLookupDTO(
                isbn,
                blankToNull(doc.path("title").asString("")),
                authors.isEmpty() ? null : authors.getFirst(),
                authors.size() > 1 ? List.copyOf(authors.subList(1, authors.size())) : List.of(),
                blankToNull(doc.path("publisher").path(0).asString("")),
                doc.path("first_publish_year").isNumber() ? Dates.parse(doc.path("first_publish_year").asString("")) : null,
                Languages.nameOf(doc.path("language").path(0).asString("")),
                doc.path("number_of_pages_median").isNumber() ? doc.path("number_of_pages_median").asInt() : null,
                blankToNull(doc.path("subject").path(0).asString("")),
                null,
                coverId > 0 ? properties.coversUrl() + "/b/id/" + coverId + "-L.jpg" : null,
                null);
        return Optional.of(new Found(book, blankToNull(doc.path("id_goodreads").path(0).asString(""))));
    }

    private JsonNode get(String path, Object... variables) {
        try {
            return client.get().uri(path, variables).retrieve().body(JsonNode.class);
        } catch (RestClientException e) {
            log.info("Open Library request {} failed: {}", path, e.getMessage());
            return null;
        }
    }

    /** Descriptions are either a plain string or {"type": ..., "value": ...}. */
    private static String description(JsonNode node) {
        String text = node.isObject() ? node.path("value").asString("") : node.asString("");
        return blankToNull(text);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
