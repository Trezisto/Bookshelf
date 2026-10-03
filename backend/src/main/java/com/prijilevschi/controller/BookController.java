package com.prijilevschi.controller;

import com.prijilevschi.ai.LlmConfig;
import com.prijilevschi.dto.BookDTO;
import com.prijilevschi.dto.BookRequest;
import com.prijilevschi.dto.ReadRequest;
import com.prijilevschi.entity.BookPhotoEntity;
import com.prijilevschi.service.BookService;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

import static com.prijilevschi.ai.LlmConfig.API_KEY_HEADER;
import static com.prijilevschi.ai.LlmConfig.BASE_URL_HEADER;
import static com.prijilevschi.ai.LlmConfig.MODEL_HEADER;

@RestController
@RequestMapping("/api/books")
public class BookController {
    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    @GetMapping
    public List<BookDTO> search(@RequestParam(required = false) String q,
                                @RequestParam(required = false) Long shelfId,
                                @RequestParam(required = false) Boolean read,
                                @RequestParam(required = false) String language) {
        return bookService.search(q, shelfId, read, language);
    }

    @GetMapping("/languages")
    public List<String> languages() {
        return bookService.languages();
    }

    @GetMapping("/{id}")
    public BookDTO get(@PathVariable Long id) {
        return bookService.get(id);
    }

    /** When the description is blank and an LLM API key header is present, a summary is generated. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BookDTO create(@Valid @RequestBody BookRequest request,
                          @RequestHeader(value = API_KEY_HEADER, required = false) String apiKey,
                          @RequestHeader(value = BASE_URL_HEADER, required = false) String baseUrl,
                          @RequestHeader(value = MODEL_HEADER, required = false) String model) {
        return bookService.create(request, new LlmConfig(apiKey, baseUrl, model));
    }

    @PutMapping("/{id}")
    public BookDTO update(@PathVariable Long id,
                          @Valid @RequestBody BookRequest request,
                          @RequestHeader(value = API_KEY_HEADER, required = false) String apiKey,
                          @RequestHeader(value = BASE_URL_HEADER, required = false) String baseUrl,
                          @RequestHeader(value = MODEL_HEADER, required = false) String model) {
        return bookService.update(id, request, new LlmConfig(apiKey, baseUrl, model));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        bookService.delete(id);
    }

    @PatchMapping("/{id}/read")
    public BookDTO setRead(@PathVariable Long id, @RequestBody ReadRequest request) {
        return bookService.setRead(id, request);
    }

    @PutMapping(value = "/{id}/cover", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void uploadCover(@PathVariable Long id, @RequestParam("file") MultipartFile file) throws IOException {
        bookService.setCover(id, file.getBytes(), file.getContentType());
    }

    @DeleteMapping("/{id}/cover")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCover(@PathVariable Long id) {
        bookService.deleteCover(id);
    }

    @GetMapping("/{id}/cover")
    public ResponseEntity<byte[]> cover(@PathVariable Long id) {
        BookPhotoEntity photo = bookService.getCover(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(photo.getPhotoContentType()))
                .cacheControl(CacheControl.noCache())
                .body(photo.getPhoto());
    }
}
