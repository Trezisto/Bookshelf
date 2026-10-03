package com.prijilevschi.error;

public class SummaryUnavailableException extends RuntimeException {
    public SummaryUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }

    public SummaryUnavailableException(String message) {
        super(message);
    }
}
