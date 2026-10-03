package com.prijilevschi.ai;

import com.prijilevschi.error.SummaryUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Asks an OpenAI-compatible LLM (Groq, OpenRouter, Gemini, OpenAI, ...) for a short book summary.
 * A chat model is built for every call because the endpoint and key come from the user's app settings.
 */
@Service
@EnableConfigurationProperties(LlmProperties.class)
public class SummaryService {

    private static final Logger log = LoggerFactory.getLogger(SummaryService.class);

    static final String UNKNOWN = "unknown";

    private static final String SYSTEM_PROMPT = """
            You are a librarian. Write a short, spoiler-free summary of the given book in 3-4 sentences.
            Describe what the book is about and its genre or tone. Reply with the summary only, no title or preamble.
            If you do not recognise the book, reply with exactly: unknown""";

    private final LlmProperties properties;

    public SummaryService(LlmProperties properties) {
        this.properties = properties;
    }

    public String summarize(String title, String author, String isbn, String language, LlmConfig config) {
        if (config == null || !config.hasApiKey()) {
            throw new SummaryUnavailableException("No LLM API key configured. Add one in the app settings.");
        }
        OpenAiChatModel model = OpenAiChatModel.builder()
                .options(OpenAiChatOptions.builder()
                        .baseUrl(orDefault(config.baseUrl(), properties.defaultBaseUrl()))
                        .apiKey(config.apiKey().trim())
                        .model(orDefault(config.model(), properties.defaultModel()))
                        .temperature(0.3)
                        .maxTokens(400)
                        .timeout(properties.timeout())
                        .maxRetries(1)
                        .build())
                .build();

        Prompt prompt = new Prompt(List.of(
                new SystemMessage(SYSTEM_PROMPT),
                new UserMessage(userPrompt(title, author, isbn, language))));
        String summary;
        try {
            ChatResponse response = model.call(prompt);
            summary = response.getResult() == null ? null : response.getResult().getOutput().getText();
        } catch (RuntimeException e) {
            log.warn("LLM call failed: {}", e.getMessage());
            throw new SummaryUnavailableException("The LLM request failed: " + e.getMessage(), e);
        }
        if (summary == null || summary.isBlank()) {
            throw new SummaryUnavailableException("The LLM returned an empty answer.");
        }
        summary = summary.strip();
        if (summary.equalsIgnoreCase(UNKNOWN) || summary.equalsIgnoreCase(UNKNOWN + ".")) {
            throw new SummaryUnavailableException("The LLM does not know this book. Please write the description yourself.");
        }
        return summary;
    }

    static String userPrompt(String title, String author, String isbn, String language) {
        StringBuilder text = new StringBuilder("Title: ").append(title.strip());
        if (author != null && !author.isBlank()) {
            text.append("\nAuthor: ").append(author.strip());
        }
        if (isbn != null && !isbn.isBlank()) {
            text.append("\nISBN: ").append(isbn.strip());
        }
        String answerLanguage = language == null || language.isBlank() ? "English" : language.strip();
        text.append("\nWrite the summary in ").append(answerLanguage).append('.');
        return text.toString();
    }

    private static String orDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.strip();
    }
}
