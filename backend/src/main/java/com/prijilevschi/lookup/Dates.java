package com.prijilevschi.lookup;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Publication dates arrive as "2005", "2005-03", "March 2005", "Mar 3, 2005" ...; partial ones become the first day. */
final class Dates {
    private Dates() {
    }

    private static final Pattern YEAR = Pattern.compile("(?<!\\d)(1[0-9]{3}|2[0-9]{3})(?!\\d)");

    private static final List<DateTimeFormatter> DAY_FORMATS = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE,
            formatter("MMMM d, uuuu"), formatter("MMM d, uuuu"), formatter("d MMMM uuuu"), formatter("d MMM uuuu"));
    private static final List<DateTimeFormatter> MONTH_FORMATS = List.of(
            formatter("uuuu-MM"), formatter("MMMM uuuu"), formatter("MMM uuuu"));

    static LocalDate parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String text = raw.strip().replace("Sept", "Sep").replaceAll("\\.", "");
        for (DateTimeFormatter format : DAY_FORMATS) {
            try {
                return LocalDate.parse(text, format);
            } catch (DateTimeParseException ignored) {
                // try the next format
            }
        }
        for (DateTimeFormatter format : MONTH_FORMATS) {
            try {
                return YearMonth.parse(text, format).atDay(1);
            } catch (DateTimeParseException ignored) {
                // try the next format
            }
        }
        Matcher year = YEAR.matcher(text);
        return year.find() ? LocalDate.of(Integer.parseInt(year.group(1)), 1, 1) : null;
    }

    private static DateTimeFormatter formatter(String pattern) {
        return new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern(pattern).toFormatter(Locale.ENGLISH);
    }
}
