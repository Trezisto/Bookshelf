package com.prijilevschi.controller;

import com.prijilevschi.ai.LlmConfig;
import com.prijilevschi.ai.SummaryService;
import com.prijilevschi.dto.SummaryRequest;
import com.prijilevschi.dto.SummaryResponse;
import com.prijilevschi.service.Isbn;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import static com.prijilevschi.ai.LlmConfig.API_KEY_HEADER;
import static com.prijilevschi.ai.LlmConfig.BASE_URL_HEADER;
import static com.prijilevschi.ai.LlmConfig.MODEL_HEADER;

@RestController
@RequestMapping("/api/ai")
public class SummaryController {
    private final SummaryService summaryService;

    public SummaryController(SummaryService summaryService) {
        this.summaryService = summaryService;
    }

    /** Preview a summary so the user can edit it before saving the book. */
    @PostMapping("/summary")
    public SummaryResponse summary(@Valid @RequestBody SummaryRequest request,
                                   @RequestHeader(value = API_KEY_HEADER, required = false) String apiKey,
                                   @RequestHeader(value = BASE_URL_HEADER, required = false) String baseUrl,
                                   @RequestHeader(value = MODEL_HEADER, required = false) String model) {
        String summary = summaryService.summarize(request.title(), request.author(), Isbn.normalize(request.isbn()),
                request.language(), new LlmConfig(apiKey, baseUrl, model));
        return new SummaryResponse(summary);
    }
}
