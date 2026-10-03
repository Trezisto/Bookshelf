package com.prijilevschi.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Read/write view of the cover photo columns of the {@code book} table.
 * Kept separate from {@link BookEntity} so the image is only loaded when the cover is requested.
 */
@Entity
@Table(name = "book")
public class BookPhotoEntity {
    @Id
    @Column(name = "id", columnDefinition = "integer")
    private Long id;

    // read with getBytes(): the SQLite driver does not implement java.sql.Blob
    @JdbcTypeCode(SqlTypes.VARBINARY)
    @Column(name = "photo", columnDefinition = "blob")
    private byte[] photo;

    @Column(name = "photo_content_type")
    private String photoContentType;

    public Long getId() {
        return id;
    }

    public byte[] getPhoto() {
        return photo;
    }

    public void setPhoto(byte[] photo) {
        this.photo = photo;
    }

    public String getPhotoContentType() {
        return photoContentType;
    }

    public void setPhotoContentType(String photoContentType) {
        this.photoContentType = photoContentType;
    }
}
