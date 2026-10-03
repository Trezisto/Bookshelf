package com.prijilevschi.controller;

import com.prijilevschi.SqliteTestSupport;
import com.prijilevschi.ai.LlmConfig;
import com.prijilevschi.ai.SummaryService;
import com.prijilevschi.error.SummaryUnavailableException;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class LibraryApiTest extends SqliteTestSupport {

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @MockitoBean
    SummaryService summaryService;

    @Test
    void createsBooksOnShelvesAndFindsThemByTitleAuthorAndIsbn() throws Exception {
        long shelf = createShelf("Living room", 3, "VERTICAL");

        mvc.perform(post("/api/books").contentType(MediaType.APPLICATION_JSON).content("""
                        {"name":"Dune","authorName":"Frank Herbert","isbn":"978-0-441-17271-9","genre":"Sci-fi",
                         "language":"English","publicationDate":"1965-05-01","pages":412,"shelfId":%d,"positionNumber":7,"depthRow":2,
                         "description":"Desert planet."}""".formatted(shelf)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.isbn").value("9780441172719"))
                .andExpect(jsonPath("$.author.name").value("Frank Herbert"))
                .andExpect(jsonPath("$.shelf.location").value("Living room"))
                .andExpect(jsonPath("$.shelf.rowNum").value(3))
                .andExpect(jsonPath("$.positionNumber").value(7))
                .andExpect(jsonPath("$.depthRow").value(2))
                .andExpect(jsonPath("$.publicationDate").value("1965-05-01"))
                .andExpect(jsonPath("$.read").value(false))
                .andExpect(jsonPath("$.hasCover").value(false));
        createBook("""
                {"name":"Children of Dune","authorName":"frank herbert"}""");
        createBook("""
                {"name":"Emma","authorName":"Jane Austen","language":"English","read":true}""");

        mvc.perform(get("/api/books").param("q", "HERBERT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
        mvc.perform(get("/api/books").param("q", "dune"))
                .andExpect(jsonPath("$[*].name", containsInAnyOrder("Dune", "Children of Dune")));
        mvc.perform(get("/api/books").param("q", "9780441172719"))
                .andExpect(jsonPath("$[*].name", Matchers.contains("Dune")));
        mvc.perform(get("/api/books").param("read", "true"))
                .andExpect(jsonPath("$[*].name", Matchers.contains("Emma")));
        mvc.perform(get("/api/books").param("language", "english"))
                .andExpect(jsonPath("$", hasSize(2)));
        mvc.perform(get("/api/books").param("shelfId", String.valueOf(shelf)))
                .andExpect(jsonPath("$[*].name", Matchers.contains("Dune")));
        mvc.perform(get("/api/books/languages"))
                .andExpect(jsonPath("$", Matchers.contains("English")));
        mvc.perform(get("/api/authors").param("q", "herb"))
                .andExpect(jsonPath("$[*].name", Matchers.contains("Frank Herbert")));
    }

    @Test
    void rejectsTakenSlotsDuplicateAndInvalidIsbns() throws Exception {
        long shelf = createShelf("Bedroom", 1, "HORIZONTAL");
        createBook("""
                {"name":"First","authorName":"A","isbn":"0306406152","shelfId":%d,"positionNumber":1}""".formatted(shelf));

        mvc.perform(post("/api/books").contentType(MediaType.APPLICATION_JSON).content("""
                        {"name":"Second","authorName":"B","shelfId":%d,"positionNumber":1}""".formatted(shelf)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail", containsString("First")));
        // same position but in the back row is fine
        createBook("""
                {"name":"Behind","authorName":"B","shelfId":%d,"positionNumber":1,"depthRow":2}""".formatted(shelf));
        mvc.perform(get("/api/shelves/{id}/next-position", shelf))
                .andExpect(jsonPath("$.positionNumber").value(2));
        mvc.perform(get("/api/shelves/{id}/next-position", shelf).param("depthRow", "2"))
                .andExpect(jsonPath("$.positionNumber").value(2));
        mvc.perform(get("/api/shelves/{id}/next-position", shelf).param("depthRow", "3"))
                .andExpect(jsonPath("$.positionNumber").value(1));

        mvc.perform(post("/api/books").contentType(MediaType.APPLICATION_JSON).content("""
                        {"name":"Copy","authorName":"A","isbn":"0-306-40615-2"}"""))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/books").contentType(MediaType.APPLICATION_JSON).content("""
                        {"name":"Bad","authorName":"A","isbn":"0306406153"}"""))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/books").contentType(MediaType.APPLICATION_JSON).content("""
                        {"name":"No slot","authorName":"A","shelfId":%d}""".formatted(shelf)))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/books").contentType(MediaType.APPLICATION_JSON).content("""
                        {"name":"","authorName":"A"}"""))
                .andExpect(status().isBadRequest());
    }

    @Test
    void storesCoAuthorsPublisherUrlAndRatingAndFindsByCoAuthor() throws Exception {
        long id = createBook("""
                {"name":"Good Omens","authorName":"Terry Pratchett","coAuthors":[" Neil Gaiman ","","neil gaiman","terry pratchett"],
                 "publisher":"Gollancz","url":"https://www.goodreads.com/book/show/12067","rating":4.5,
                 "publicationDate":"1990-05-01"}""");

        mvc.perform(get("/api/books/{id}", id))
                .andExpect(jsonPath("$.coAuthors", Matchers.contains("Neil Gaiman")))
                .andExpect(jsonPath("$.publisher").value("Gollancz"))
                .andExpect(jsonPath("$.url").value("https://www.goodreads.com/book/show/12067"))
                .andExpect(jsonPath("$.rating").value(4.5));
        mvc.perform(get("/api/books").param("q", "gaiman"))
                .andExpect(jsonPath("$[*].name", Matchers.contains("Good Omens")));

        mvc.perform(put("/api/books/{id}", id).contentType(MediaType.APPLICATION_JSON).content("""
                        {"name":"Good Omens","authorName":"Terry Pratchett","coAuthors":[]}"""))
                .andExpect(jsonPath("$.coAuthors", empty()))
                .andExpect(jsonPath("$.rating").value(nullValue()));
        mvc.perform(get("/api/books").param("q", "gaiman")).andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void rejectsOutOfRangeRatingAndNonHttpUrl() throws Exception {
        mvc.perform(post("/api/books").contentType(MediaType.APPLICATION_JSON).content("""
                        {"name":"X","authorName":"A","rating":5.5}"""))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/books").contentType(MediaType.APPLICATION_JSON).content("""
                        {"name":"X","authorName":"A","url":"javascript:alert(1)"}"""))
                .andExpect(status().isBadRequest());
    }

    @Test
    void recordsAddedAndModifiedDatesAndRevisionHistory() throws Exception {
        long id = createBook("""
                {"name":"Draft","authorName":"A","publicationDate":"2001-01-01"}""");
        MvcResult created = mvc.perform(get("/api/books/{id}", id))
                .andExpect(jsonPath("$.createdAt", notNullValue()))
                .andExpect(jsonPath("$.modifiedAt", notNullValue()))
                .andReturn();
        String createdAt = com.jayway.jsonpath.JsonPath.read(created.getResponse().getContentAsString(), "$.createdAt");

        Thread.sleep(20);
        mvc.perform(put("/api/books/{id}", id).contentType(MediaType.APPLICATION_JSON).content("""
                        {"name":"Final","authorName":"A","publicationDate":"2001-01-01","rating":3}"""))
                .andExpect(jsonPath("$.createdAt").value(createdAt))
                .andExpect(jsonPath("$.modifiedAt", not(createdAt)));

        mvc.perform(get("/api/books/{id}/history", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].type").value("ADD"))
                .andExpect(jsonPath("$[0].name").value("Draft"))
                .andExpect(jsonPath("$[1].type").value("MOD"))
                .andExpect(jsonPath("$[1].name").value("Final"))
                .andExpect(jsonPath("$[1].rating").value(3.0));

        mvc.perform(delete("/api/books/{id}", id)).andExpect(status().isNoContent());
        mvc.perform(get("/api/books/{id}/history", id))
                .andExpect(jsonPath("$[2].type").value("DEL"));
        mvc.perform(get("/api/books/{id}/history", 987654)).andExpect(status().isNotFound());
    }

    @Test
    void togglesReadAndDefaultsDateToToday() throws Exception {
        long id = createBook("""
                {"name":"Ulysses","authorName":"James Joyce"}""");

        mvc.perform(patch("/api/books/{id}/read", id).contentType(MediaType.APPLICATION_JSON).content("""
                        {"read":true}"""))
                .andExpect(jsonPath("$.read").value(true))
                .andExpect(jsonPath("$.dateRead").value(LocalDate.now().toString()));
        mvc.perform(patch("/api/books/{id}/read", id).contentType(MediaType.APPLICATION_JSON).content("""
                        {"read":true,"dateRead":"2024-02-29"}"""))
                .andExpect(jsonPath("$.dateRead").value("2024-02-29"));
        // stored as ISO text in SQLite
        org.assertj.core.api.Assertions.assertThat(
                jdbc.queryForObject("select date_read from book where id = ?", String.class, id)).isEqualTo("2024-02-29");
        mvc.perform(patch("/api/books/{id}/read", id).contentType(MediaType.APPLICATION_JSON).content("""
                        {"read":false,"dateRead":"2024-02-29"}"""))
                .andExpect(jsonPath("$.read").value(false))
                .andExpect(jsonPath("$.dateRead").value(nullValue()));
    }

    @Test
    void storesAndServesTheCover() throws Exception {
        long id = createBook("""
                {"name":"Covered","authorName":"Painter"}""");
        byte[] jpeg = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 1, 2, 3};

        mvc.perform(multipart("/api/books/{id}/cover", id)
                        .file(new MockMultipartFile("file", "cover.jpg", "image/jpeg", jpeg))
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        }))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/books/{id}/cover", id))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/jpeg"))
                .andExpect(content().bytes(jpeg));
        mvc.perform(get("/api/books/{id}", id)).andExpect(jsonPath("$.hasCover").value(true));
        // the list view doesn't break and still reports the cover
        mvc.perform(get("/api/books").param("q", "Covered")).andExpect(jsonPath("$[0].hasCover").value(true));

        // editing the book keeps the photo
        mvc.perform(put("/api/books/{id}", id).contentType(MediaType.APPLICATION_JSON).content("""
                        {"name":"Covered (2nd ed.)","authorName":"Painter"}"""))
                .andExpect(status().isOk());
        mvc.perform(get("/api/books/{id}/cover", id)).andExpect(content().bytes(jpeg));

        mvc.perform(multipart("/api/books/{id}/cover", id)
                        .file(new MockMultipartFile("file", "x.gif", "image/gif", jpeg))
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        }))
                .andExpect(status().isBadRequest());

        mvc.perform(delete("/api/books/{id}/cover", id)).andExpect(status().isNoContent());
        mvc.perform(get("/api/books/{id}/cover", id)).andExpect(status().isNotFound());
    }

    @Test
    void generatesSummaryOnlyWhenDescriptionIsBlankAndKeyIsPresent() throws Exception {
        when(summaryService.summarize(eq("Dracula"), eq("Bram Stoker"), any(), any(), any()))
                .thenReturn("A vampire travels to England.");

        // no key header: saved without description, LLM never called
        mvc.perform(post("/api/books").contentType(MediaType.APPLICATION_JSON).content("""
                        {"name":"Dracula","authorName":"Bram Stoker"}"""))
                .andExpect(jsonPath("$.description").value(nullValue()));
        verifyNoInteractions(summaryService);

        mvc.perform(post("/api/books").contentType(MediaType.APPLICATION_JSON)
                        .header(LlmConfig.API_KEY_HEADER, "k").header(LlmConfig.MODEL_HEADER, "m")
                        .content("""
                                {"name":"Dracula","authorName":"Bram Stoker"}"""))
                .andExpect(jsonPath("$.description").value("A vampire travels to England."));
        verify(summaryService).summarize(eq("Dracula"), eq("Bram Stoker"), isNull(), isNull(),
                eq(new LlmConfig("k", null, "m")));

        // user-provided description wins
        mvc.perform(post("/api/books").contentType(MediaType.APPLICATION_JSON)
                        .header(LlmConfig.API_KEY_HEADER, "k")
                        .content("""
                                {"name":"Dracula","authorName":"Bram Stoker","description":"Mine."}"""))
                .andExpect(jsonPath("$.description").value("Mine."));
        verifyNoMoreInteractions(summaryService);

        // LLM failure doesn't block saving
        when(summaryService.summarize(eq("Obscure"), any(), any(), any(), any()))
                .thenThrow(new SummaryUnavailableException("unknown book"));
        mvc.perform(post("/api/books").contentType(MediaType.APPLICATION_JSON)
                        .header(LlmConfig.API_KEY_HEADER, "k")
                        .content("""
                                {"name":"Obscure","authorName":"Nobody"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.description").value(nullValue()));
    }

    @Test
    void summaryPreviewEndpointMapsFailuresTo502() throws Exception {
        when(summaryService.summarize(eq("Dune"), any(), any(), any(), any())).thenReturn("Spice.");
        when(summaryService.summarize(eq("Nope"), any(), any(), any(), any()))
                .thenThrow(new SummaryUnavailableException("no key"));

        mvc.perform(post("/api/ai/summary").contentType(MediaType.APPLICATION_JSON)
                        .header(LlmConfig.API_KEY_HEADER, "k")
                        .content("""
                                {"title":"Dune","author":"Frank Herbert","isbn":"978-0-441-17271-9"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary").value("Spice."));
        verify(summaryService).summarize(eq("Dune"), eq("Frank Herbert"), eq("9780441172719"), isNull(), any());
        mvc.perform(post("/api/ai/summary").contentType(MediaType.APPLICATION_JSON).content("""
                        {"title":"Nope"}"""))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.detail").value("no key"));
    }

    @Test
    void deletingShelfKeepsItsBooksUnshelved() throws Exception {
        long shelf = createShelf("Hall", 2, "VERTICAL");
        long book = createBook("""
                {"name":"Kept","authorName":"Keeper","shelfId":%d,"positionNumber":4}""".formatted(shelf));

        mvc.perform(post("/api/shelves").contentType(MediaType.APPLICATION_JSON).content("""
                        {"location":"hall","rowNum":2,"orientation":"VERTICAL"}"""))
                .andExpect(status().isConflict());
        mvc.perform(get("/api/shelves/{id}/books", shelf)).andExpect(jsonPath("$[*].name", Matchers.contains("Kept")));

        mvc.perform(delete("/api/shelves/{id}", shelf)).andExpect(status().isNoContent());
        mvc.perform(get("/api/books/{id}", book))
                .andExpect(jsonPath("$.shelf").value(nullValue()))
                .andExpect(jsonPath("$.positionNumber").value(nullValue()));
        mvc.perform(get("/api/shelves/{id}", shelf)).andExpect(status().isNotFound());
    }

    private long createShelf(String location, int row, String orientation) throws Exception {
        MvcResult result = mvc.perform(post("/api/shelves").contentType(MediaType.APPLICATION_JSON).content("""
                        {"location":"%s","rowNum":%d,"orientation":"%s"}""".formatted(location, row, orientation)))
                .andExpect(status().isCreated())
                .andReturn();
        return idOf(result);
    }

    private long createBook(String json) throws Exception {
        MvcResult result = mvc.perform(post("/api/books").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated())
                .andReturn();
        return idOf(result);
    }

    private static long idOf(MvcResult result) throws Exception {
        String body = result.getResponse().getContentAsString();
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\"id\":(\\d+)").matcher(body);
        if (!m.find()) {
            throw new AssertionError("No id in " + body);
        }
        return Long.parseLong(m.group(1));
    }
}
