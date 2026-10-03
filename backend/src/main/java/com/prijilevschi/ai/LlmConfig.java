package com.prijilevschi.ai;

/**
 * Per-request connection details for an OpenAI-compatible endpoint. The API key comes from the
 * app's settings with each request and is never stored on the server.
 */
public record LlmConfig(String apiKey, String baseUrl, String model) {

    public static final String API_KEY_HEADER = "X-LLM-Api-Key";
    public static final String BASE_URL_HEADER = "X-LLM-Base-Url";
    public static final String MODEL_HEADER = "X-LLM-Model";

    public boolean hasApiKey() {
        return apiKey != null && !apiKey.isBlank();
    }
}
