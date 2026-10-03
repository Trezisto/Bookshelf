package com.prijilevschi.lookup;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** Turns ISO 639 codes ("en", "eng", MARC "fre") into English language names, e.g. "English". */
final class Languages {
    private Languages() {
    }

    /** MARC/bibliographic codes that differ from the terminological ones (as in {@link Locale#getISO3Language()}). */
    private static final Map<String, String> ALIASES = Map.of(
            "fre", "fra", "ger", "deu", "rum", "ron", "dut", "nld", "gre", "ell",
            "chi", "zho", "cze", "ces", "per", "fas", "slo", "slk", "ice", "isl");

    private static final Map<String, String> NAMES = new HashMap<>();

    static {
        for (String iso2 : Locale.getISOLanguages()) {
            Locale locale = Locale.of(iso2);
            String name = locale.getDisplayLanguage(Locale.ENGLISH);
            NAMES.put(iso2, name);
            NAMES.put(locale.getISO3Language(), name);
        }
    }

    static String nameOf(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        String normalized = code.strip().toLowerCase(Locale.ROOT);
        normalized = normalized.substring(normalized.lastIndexOf('/') + 1);
        return NAMES.get(ALIASES.getOrDefault(normalized, normalized));
    }
}
