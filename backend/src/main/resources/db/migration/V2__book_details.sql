-- Publisher, link, rating, co-authors; year becomes a full publication date; add/modify timestamps

ALTER TABLE book ADD COLUMN publisher VARCHAR(255);
ALTER TABLE book ADD COLUMN url TEXT;
ALTER TABLE book ADD COLUMN rating DOUBLE CHECK (rating IS NULL OR (rating >= 0 AND rating <= 5));

-- ISO-8601 date; a year-only value is stored as January 1st
ALTER TABLE book ADD COLUMN publication_date VARCHAR(255);
UPDATE book SET publication_date = printf('%04d-01-01', publish_year) WHERE publish_year IS NOT NULL;
ALTER TABLE book DROP COLUMN publish_year;

-- ISO-8601 instants (UTC), maintained by Spring Data auditing
ALTER TABLE book ADD COLUMN created_at VARCHAR(255);
ALTER TABLE book ADD COLUMN modified_at VARCHAR(255);
UPDATE book SET created_at = strftime('%Y-%m-%dT%H:%M:%SZ', 'now'), modified_at = strftime('%Y-%m-%dT%H:%M:%SZ', 'now');

-- Additional authors, in the order they were entered
CREATE TABLE book_co_author (
    book_id   INTEGER      NOT NULL REFERENCES book (id) ON DELETE CASCADE,
    position  INTEGER      NOT NULL,
    co_author VARCHAR(255) NOT NULL,
    PRIMARY KEY (book_id, position)
);
