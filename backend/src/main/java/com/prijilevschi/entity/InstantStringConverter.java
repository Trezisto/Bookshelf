package com.prijilevschi.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.time.Instant;

/** SQLite has no timestamp type; store instants as ISO-8601 UTC text (2026-10-03T12:00:00Z). */
@Converter
public class InstantStringConverter implements AttributeConverter<Instant, String> {
    @Override
    public String convertToDatabaseColumn(Instant instant) {
        return instant == null ? null : instant.toString();
    }

    @Override
    public Instant convertToEntityAttribute(String value) {
        return value == null || value.isBlank() ? null : Instant.parse(value);
    }
}
