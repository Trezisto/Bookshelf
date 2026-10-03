package com.prijilevschi.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.time.LocalDate;

/** SQLite has no date type; store dates as ISO-8601 text (yyyy-MM-dd) so they stay readable and sortable. */
@Converter
public class LocalDateStringConverter implements AttributeConverter<LocalDate, String> {
    @Override
    public String convertToDatabaseColumn(LocalDate date) {
        return date == null ? null : date.toString();
    }

    @Override
    public LocalDate convertToEntityAttribute(String value) {
        return value == null || value.isBlank() ? null : LocalDate.parse(value);
    }
}
