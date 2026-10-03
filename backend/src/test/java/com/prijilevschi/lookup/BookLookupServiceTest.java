package com.prijilevschi.lookup;

import com.prijilevschi.dto.BookLookupDTO;
import com.prijilevschi.dto.BookRequest;
import mockwebserver3.Dispatcher;
import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;
import mockwebserver3.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;

class BookLookupServiceTest {

    private static final String DUNE_EDITION = """
            {"title":"Dune","publishers":["Ace Books"],"publish_date":"August 1, 2005","number_of_pages":544,
             "languages":[{"key":"/languages/eng"}],"covers":[123],
             "authors":[{"key":"/authors/OL1A"},{"key":"/authors/OL2A"}],"works":[{"key":"/works/OL9W"}],
             "identifiers":{"goodreads":["234225"]}}""";

    private MockWebServer server;
    private final Map<String, MockResponse> routes = new ConcurrentHashMap<>();
    private BookLookupService service;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                String path = request.getUrl().encodedPath();
                return routes.getOrDefault(path, new MockResponse.Builder().code(404).build());
            }
        });
        server.start();
        String base = server.url("/").toString().replaceAll("/$", "");
        LookupProperties properties = new LookupProperties(base, base + "/books/v1", null, "https://covers.test",
                base + "/html/", Duration.ofSeconds(2), Duration.ofSeconds(2));
        service = new BookLookupService(new OpenLibraryClient(properties), new GoogleBooksClient(properties),
                new GoodreadsUrlResolver(properties));
    }

    @AfterEach
    void tearDown() {
        server.close();
    }

    @Test
    void combinesEditionAuthorsWorkAndTheGoodreadsIdOfOpenLibrary() {
        json("/isbn/9780441172719.json", DUNE_EDITION);
        json("/authors/OL1A.json", "{\"name\":\"Frank Herbert\"}");
        json("/authors/OL2A.json", "{\"name\":\"Brian Herbert\"}");
        json("/works/OL9W.json", "{\"description\":{\"type\":\"/type/text\",\"value\":\"Desert planet.\"},\"subjects\":[\"Science fiction\",\"Ecology\"]}");

        BookLookupDTO book = service.byIsbn("9780441172719").orElseThrow();

        assertThat(book.title()).isEqualTo("Dune");
        assertThat(book.author()).isEqualTo("Frank Herbert");
        assertThat(book.coAuthors()).containsExactly("Brian Herbert");
        assertThat(book.publisher()).isEqualTo("Ace Books");
        assertThat(book.publicationDate()).isEqualTo(LocalDate.of(2005, 8, 1));
        assertThat(book.language()).isEqualTo("English");
        assertThat(book.pages()).isEqualTo(544);
        assertThat(book.genre()).isEqualTo("Science fiction");
        assertThat(book.description()).isEqualTo("Desert planet.");
        assertThat(book.coverUrl()).isEqualTo("https://covers.test/b/isbn/9780441172719-L.jpg");
        assertThat(book.url()).isEqualTo("https://www.goodreads.com/book/show/234225");
    }

    @Test
    void googleBooksFillsWhatOpenLibraryLacksAndTheLinkComesFromAWebSearch() {
        json("/isbn/9780441172719.json", "{\"title\":\"Dune\",\"publish_date\":\"1990\"}");
        json("/books/v1/volumes", """
                {"items":[{"volumeInfo":{"title":"Dune","authors":["Frank Herbert","Brian Herbert"],"publisher":"Penguin",
                  "publishedDate":"2010-03","pageCount":612,"language":"en","categories":["Fiction"],
                  "description":"A classic.","imageLinks":{"thumbnail":"http://books.google.test/cover.jpg"}}}]}""");
        html("/html/", "<a class=\"result__a\" href=\"//duckduckgo.com/l/?uddg=https%3A%2F%2Fwww.goodreads.com%2Fbook%2Fshow%2F234225.Dune&rut=x\">Dune</a>");

        BookLookupDTO book = service.byIsbn("9780441172719").orElseThrow();

        assertThat(book.publicationDate()).isEqualTo(LocalDate.of(1990, 1, 1)); // Open Library wins
        assertThat(book.author()).isEqualTo("Frank Herbert");
        assertThat(book.coAuthors()).containsExactly("Brian Herbert");
        assertThat(book.publisher()).isEqualTo("Penguin");
        assertThat(book.pages()).isEqualTo(612);
        assertThat(book.language()).isEqualTo("English");
        assertThat(book.genre()).isEqualTo("Fiction");
        assertThat(book.coverUrl()).isEqualTo("https://books.google.test/cover.jpg");
        assertThat(book.url()).isEqualTo("https://www.goodreads.com/book/show/234225");
    }

    @Test
    void unknownBookAndBrokenSourcesGiveAnEmptyResultNotAnError() {
        routes.put("/isbn/9780441172719.json", new MockResponse.Builder().code(500).build());
        routes.put("/books/v1/volumes", new MockResponse.Builder().code(503).build());

        assertThat(service.byIsbn("9780441172719")).isEmpty();
        assertThat(service.byTitleAuthor("Nothing", null)).isEmpty();
        assertThat(service.goodreadsUrl("9780441172719", "Dune", "Frank Herbert")).isEmpty();
    }

    @Test
    void findsByTitleAndAuthorWithTheIsbnAndGoodreadsIdOfTheBestMatch() {
        json("/search.json", """
                {"docs":[{"title":"Dune","author_name":["Frank Herbert","Brian Herbert"],"first_publish_year":1965,
                  "publisher":["Chilton"],"language":["eng","fre"],"number_of_pages_median":412,
                  "isbn":["0441172717","9780441172719"],"subject":["Science fiction"],"cover_i":77,"id_goodreads":["234225"]}]}""");

        BookLookupDTO book = service.byTitleAuthor("Dune", "Frank Herbert").orElseThrow();

        assertThat(book.isbn()).isEqualTo("9780441172719");
        assertThat(book.author()).isEqualTo("Frank Herbert");
        assertThat(book.coAuthors()).containsExactly("Brian Herbert");
        assertThat(book.publicationDate()).isEqualTo(LocalDate.of(1965, 1, 1));
        assertThat(book.coverUrl()).isEqualTo("https://covers.test/b/id/77-L.jpg");
        assertThat(book.url()).isEqualTo("https://www.goodreads.com/book/show/234225");
    }

    @Test
    void goodreadsUrlFallsBackFromIsbnToWebSearchByTitle() {
        html("/html/", "<a href=\"https://www.goodreads.com/book/show/5107.The_Catcher_in_the_Rye\">x</a>");

        Optional<String> url = service.goodreadsUrl(null, "The Catcher in the Rye", "J. D. Salinger");

        assertThat(url).contains("https://www.goodreads.com/book/show/5107");
    }

    @Test
    void newBookWithoutLinkGetsOneButAnEnteredLinkIsKept() {
        json("/isbn/9780441172719.json", DUNE_EDITION);
        BookRequest bare = request(null);
        BookRequest linked = request("https://example.com/mine");

        assertThat(service.withGoodreadsUrl(bare).url()).isEqualTo("https://www.goodreads.com/book/show/234225");
        assertThat(service.withGoodreadsUrl(linked)).isSameAs(linked);
        // nothing found: request is returned unchanged
        assertThat(service.withGoodreadsUrl(new BookRequest("Unknown", "Nobody", null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null)).url()).isNull();
    }

    @Test
    void parsesPartialAndWordedPublicationDates() {
        assertThat(Dates.parse("2005")).isEqualTo(LocalDate.of(2005, 1, 1));
        assertThat(Dates.parse("2005-03")).isEqualTo(LocalDate.of(2005, 3, 1));
        assertThat(Dates.parse("2005-03-17")).isEqualTo(LocalDate.of(2005, 3, 17));
        assertThat(Dates.parse("March 2005")).isEqualTo(LocalDate.of(2005, 3, 1));
        assertThat(Dates.parse("Mar 17, 2005")).isEqualTo(LocalDate.of(2005, 3, 17));
        assertThat(Dates.parse("17 March 2005")).isEqualTo(LocalDate.of(2005, 3, 17));
        assertThat(Dates.parse("[c1999]")).isEqualTo(LocalDate.of(1999, 1, 1));
        assertThat(Dates.parse("n.d.")).isNull();
        assertThat(Dates.parse(" ")).isNull();
    }

    @Test
    void mapsLanguageCodes() {
        assertThat(Languages.nameOf("/languages/eng")).isEqualTo("English");
        assertThat(Languages.nameOf("fre")).isEqualTo("French");
        assertThat(Languages.nameOf("ro")).isEqualTo("Romanian");
        assertThat(Languages.nameOf("")).isNull();
    }

    private static BookRequest request(String url) {
        return new BookRequest("Dune", "Frank Herbert", null, "9780441172719", null, null, null, null, url, null,
                null, null, null, null, null, null, null);
    }

    private void json(String path, String body) {
        routes.put(path, new MockResponse.Builder().code(200).addHeader("Content-Type", "application/json").body(body).build());
    }

    private void html(String path, String body) {
        routes.put(path, new MockResponse.Builder().code(200).addHeader("Content-Type", "text/html").body(body).build());
    }
}
