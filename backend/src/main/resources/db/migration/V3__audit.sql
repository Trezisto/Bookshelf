-- Hibernate Envers audit tables. SQLite has no sequences, so revinfo uses an autoincrement id.

CREATE TABLE revinfo (
    rev      INTEGER PRIMARY KEY AUTOINCREMENT,
    revtstmp BIGINT NOT NULL   -- epoch milliseconds
);

CREATE TABLE book_aud (
    id               INTEGER  NOT NULL,
    rev              INTEGER  NOT NULL REFERENCES revinfo (rev),
    revtype          SMALLINT,          -- 0 = added, 1 = modified, 2 = deleted
    name             VARCHAR(255),
    description      TEXT,
    isbn             VARCHAR(255),
    genre            VARCHAR(255),
    language         VARCHAR(255),
    publisher        VARCHAR(255),
    url              TEXT,
    rating           DOUBLE,
    publication_date VARCHAR(255),
    pages            INTEGER,
    is_read          BOOLEAN,
    date_read        VARCHAR(255),
    author_id        INTEGER,
    shelf_id         INTEGER,
    position_number  INTEGER,
    depth_row        INTEGER,
    has_photo        BOOLEAN,
    PRIMARY KEY (id, rev)
);

CREATE TABLE book_co_author_aud (
    rev       INTEGER      NOT NULL REFERENCES revinfo (rev),
    book_id   INTEGER      NOT NULL,
    position  INTEGER      NOT NULL,
    co_author VARCHAR(255) NOT NULL,
    revtype   SMALLINT,
    PRIMARY KEY (rev, book_id, position)
);

-- Baseline: books that exist before auditing started get an "added" revision so their history is never empty
INSERT INTO revinfo (revtstmp)
SELECT CAST(strftime('%s', 'now') AS INTEGER) * 1000 WHERE EXISTS (SELECT 1 FROM book);

INSERT INTO book_aud (id, rev, revtype, name, description, isbn, genre, language, publisher, url, rating,
                      publication_date, pages, is_read, date_read, author_id, shelf_id, position_number,
                      depth_row, has_photo)
SELECT id, (SELECT max(rev) FROM revinfo), 0, name, description, isbn, genre, language, publisher, url, rating,
       publication_date, pages, is_read, date_read, author_id, shelf_id, position_number, depth_row, has_photo
FROM book;
