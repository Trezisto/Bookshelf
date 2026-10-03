package com.prijilevschi.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "author")
public class AuthorEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", columnDefinition = "integer") // SQLite rowid alias
    private Long id;

    @Column(name = "name", nullable = false, unique = true)
    private String name;

    protected AuthorEntity() {
    }

    public AuthorEntity(String name) {
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
