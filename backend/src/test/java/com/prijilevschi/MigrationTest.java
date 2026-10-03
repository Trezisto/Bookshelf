package com.prijilevschi;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;

/** Upgrades a database that holds books written by the first schema version. */
class MigrationTest {

    @TempDir
    Path dir;

    @Test
    void upgradesExistingBooksToPublicationDateTimestampsAndBaselineRevision() throws Exception {
        String url = "jdbc:sqlite:" + dir.resolve("old.db") + "?foreign_keys=on";
        Flyway.configure().dataSource(url, null, null).target("1").load().migrate();
        try (Connection c = DriverManager.getConnection(url); Statement s = c.createStatement()) {
            s.executeUpdate("insert into author (name) values ('Frank Herbert')");
            s.executeUpdate("insert into book (name, author_id, publish_year) values ('Dune', 1, 1965)");
            s.executeUpdate("insert into book (name, author_id) values ('Undated', 1)");
        }

        Flyway.configure().dataSource(url, null, null).load().migrate();

        try (Connection c = DriverManager.getConnection(url); Statement s = c.createStatement()) {
            ResultSet books = s.executeQuery(
                    "select name, publication_date, created_at, modified_at from book order by id");
            assertThat(books.next()).isTrue();
            assertThat(books.getString("publication_date")).isEqualTo("1965-01-01");
            assertThat(books.getString("created_at")).endsWith("Z");
            assertThat(books.getString("modified_at")).isNotNull();
            assertThat(books.next()).isTrue();
            assertThat(books.getString("publication_date")).isNull();

            ResultSet baseline = s.executeQuery("select count(*), min(revtype), max(revtype) from book_aud");
            assertThat(baseline.next()).isTrue();
            assertThat(baseline.getInt(1)).isEqualTo(2);
            assertThat(baseline.getInt(2)).isZero();
            assertThat(baseline.getInt(3)).isZero();
        }
    }

    @Test
    void emptyDatabaseGetsNoBaselineRevision() throws Exception {
        String url = "jdbc:sqlite:" + dir.resolve("new.db") + "?foreign_keys=on";
        Flyway.configure().dataSource(url, null, null).load().migrate();
        try (Connection c = DriverManager.getConnection(url); Statement s = c.createStatement()) {
            ResultSet rs = s.executeQuery("select count(*) from revinfo");
            assertThat(rs.next()).isTrue();
            assertThat(rs.getInt(1)).isZero();
        }
    }
}
