package com.prijilevschi.dto;

import java.time.LocalDate;

public record ReadRequest(Boolean read, LocalDate dateRead) {
    public boolean isRead() {
        return Boolean.TRUE.equals(read);
    }
}
