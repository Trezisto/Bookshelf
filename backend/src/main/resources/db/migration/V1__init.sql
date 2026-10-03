-- Home library schema (SQLite)

CREATE TABLE author (
    id   INTEGER PRIMARY KEY AUTOINCREMENT,
    name VARCHAR(255) NOT NULL UNIQUE COLLATE NOCASE
);

CREATE TABLE shelf (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    location    VARCHAR(255) NOT NULL,
    row_num     INTEGER      NOT NULL CHECK (row_num >= 1),
    orientation VARCHAR(255) NOT NULL DEFAULT 'VERTICAL' CHECK (orientation IN ('HORIZONTAL', 'VERTICAL'))
);
CREATE UNIQUE INDEX ux_shelf_location_row ON shelf (location COLLATE NOCASE, row_num);

CREATE TABLE book (
    id                 INTEGER PRIMARY KEY AUTOINCREMENT,
    name               VARCHAR(255) NOT NULL,
    description        TEXT,
    photo              BLOB,
    photo_content_type VARCHAR(255),
    has_photo          BOOLEAN      NOT NULL DEFAULT 0,
    isbn               VARCHAR(255) UNIQUE,
    genre              VARCHAR(255),
    language           VARCHAR(255),
    publish_year       INTEGER,
    pages              INTEGER CHECK (pages IS NULL OR pages >= 1),
    is_read            BOOLEAN      NOT NULL DEFAULT 0,
    date_read          VARCHAR(255),                 -- ISO-8601 date, e.g. 2026-10-03
    author_id          INTEGER      NOT NULL REFERENCES author (id),
    shelf_id           INTEGER      REFERENCES shelf (id) ON DELETE SET NULL,
    position_number    INTEGER CHECK (position_number IS NULL OR position_number >= 1),
    depth_row          INTEGER      NOT NULL DEFAULT 1 CHECK (depth_row >= 1)
);
-- One book per slot: shelf + depth row (1 = front) + position from the left
CREATE UNIQUE INDEX ux_book_slot ON book (shelf_id, depth_row, position_number);
CREATE INDEX ix_book_author ON book (author_id);
CREATE INDEX ix_book_name ON book (name COLLATE NOCASE);
