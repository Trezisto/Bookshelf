package com.prijilevschi.error;

/** The app asked for an LLM base URL that this server does not allow; answered with 400. */
public class LlmEndpointNotAllowedException extends SummaryUnavailableException {
    public LlmEndpointNotAllowedException(String message) {
        super(message);
    }
}
