package com.prijilevschi.ai;

import com.prijilevschi.error.SummaryUnavailableException;
import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;
import mockwebserver3.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SummaryServiceTest {

    private MockWebServer server;
    private SummaryService service;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        service = new SummaryService(new LlmProperties("http://unused.invalid/v1", "default-model", Duration.ofSeconds(10)));
    }

    @AfterEach
    void tearDown() {
        server.close();
    }

    @Test
    void sendsKeyModelAndBookToTheConfiguredEndpoint() throws InterruptedException {
        server.enqueue(completion("A young wizard discovers his heritage."));
        LlmConfig config = new LlmConfig("secret-key", server.url("/v1").toString(), "llama-test");

        String summary = service.summarize("Harry Potter", "J. K. Rowling", "9780747532699", "Romanian", config);

        assertThat(summary).isEqualTo("A young wizard discovers his heritage.");
        RecordedRequest request = server.takeRequest(5, TimeUnit.SECONDS);
        assertThat(request).isNotNull();
        assertThat(request.getUrl().encodedPath()).isEqualTo("/v1/chat/completions");
        assertThat(request.getHeaders().get("Authorization")).isEqualTo("Bearer secret-key");
        String body = request.getBody().utf8();
        assertThat(body).contains("\"model\":\"llama-test\"")
                .contains("Harry Potter")
                .contains("J. K. Rowling")
                .contains("9780747532699")
                .contains("Romanian");
    }

    @Test
    void unknownBookIsReportedAsUnavailable() {
        server.enqueue(completion("unknown"));
        LlmConfig config = new LlmConfig("key", server.url("/v1").toString(), null);

        assertThatThrownBy(() -> service.summarize("Some Private Notebook", null, null, null, config))
                .isInstanceOf(SummaryUnavailableException.class)
                .hasMessageContaining("does not know");
    }

    @Test
    void httpErrorIsReportedAsUnavailable() {
        server.enqueue(new MockResponse.Builder().code(401)
                .addHeader("Content-Type", "application/json")
                .body("{\"error\":{\"message\":\"Invalid API Key\",\"type\":\"invalid_request_error\"}}")
                .build());
        LlmConfig config = new LlmConfig("bad", server.url("/v1").toString(), "m");

        assertThatThrownBy(() -> service.summarize("Dune", "Frank Herbert", null, null, config))
                .isInstanceOf(SummaryUnavailableException.class);
    }

    @Test
    void missingKeyIsRejectedWithoutCallingTheLlm() {
        assertThatThrownBy(() -> service.summarize("Dune", null, null, null, new LlmConfig(" ", null, null)))
                .isInstanceOf(SummaryUnavailableException.class);
        assertThat(server.getRequestCount()).isZero();
    }

    private static MockResponse completion(String text) {
        String json = """
                {"id":"chatcmpl-1","object":"chat.completion","created":1700000000,"model":"llama-test",
                 "choices":[{"index":0,"finish_reason":"stop","logprobs":null,
                   "message":{"role":"assistant","content":"%s","refusal":null}}],
                 "usage":{"prompt_tokens":10,"completion_tokens":10,"total_tokens":20}}
                """.formatted(text);
        return new MockResponse.Builder().code(200).addHeader("Content-Type", "application/json").body(json).build();
    }
}
