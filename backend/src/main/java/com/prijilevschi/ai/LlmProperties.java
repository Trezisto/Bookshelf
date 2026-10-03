package com.prijilevschi.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties("library.llm")
public record LlmProperties(String defaultBaseUrl, String defaultModel, Duration timeout) {
}
