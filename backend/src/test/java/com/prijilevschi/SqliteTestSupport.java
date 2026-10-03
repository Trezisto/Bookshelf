package com.prijilevschi;

import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.nio.file.Path;

/** Every test class gets its own SQLite file, migrated by Flyway and validated by Hibernate. */
public abstract class SqliteTestSupport {

    @TempDir
    static Path dbDir;

    @DynamicPropertySource
    static void sqlite(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",
                () -> "jdbc:sqlite:" + dbDir.resolve("test.db") + "?foreign_keys=on&journal_mode=WAL");
    }
}
