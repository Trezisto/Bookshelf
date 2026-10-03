package com.prijilevschi.entity;

import jakarta.persistence.*;
import org.hibernate.envers.RevisionEntity;
import org.hibernate.envers.RevisionNumber;
import org.hibernate.envers.RevisionTimestamp;

import java.time.Instant;

/** One Envers revision. SQLite has no sequences, so the number comes from an autoincrement column. */
@Entity
@Table(name = "revinfo")
@RevisionEntity
public class BookRevisionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @RevisionNumber
    @Column(name = "rev", columnDefinition = "integer")
    private Long id;

    @RevisionTimestamp
    @Column(name = "revtstmp")
    private long timestamp;

    public Long getId() {
        return id;
    }

    public Instant getTimestamp() {
        return Instant.ofEpochMilli(timestamp);
    }
}
