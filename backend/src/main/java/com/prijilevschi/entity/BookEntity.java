package com.prijilevschi.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.BatchSize;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.hibernate.envers.RelationTargetAuditMode;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * A book and where it stands: shelf (location + row), position from the left and depth row (1 = front).
 * The cover photo lives in the same table but is mapped by {@link BookPhotoEntity},
 * so loading books for the shelves view never pulls the image bytes.
 * Every change is recorded by Hibernate Envers; the author and shelf are referenced by id only.
 */
@Entity
@Table(name = "book")
@Audited
@EntityListeners(AuditingEntityListener.class)
public class BookEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", columnDefinition = "integer") // SQLite rowid alias
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "isbn", unique = true)
    private String isbn;

    @Column(name = "genre")
    private String genre;

    @Column(name = "language")
    private String language;

    @Column(name = "publisher")
    private String publisher;

    @Column(name = "url")
    private String url;

    /** Own rating, 0-5. */
    @Column(name = "rating")
    private Double rating;

    /** A lookup that only knows the year or month is stored as the first day of it. */
    @Convert(converter = LocalDateStringConverter.class)
    @Column(name = "publication_date")
    private LocalDate publicationDate;

    @Column(name = "pages")
    private Integer pages;

    @Column(name = "is_read", nullable = false)
    private boolean read;

    @Convert(converter = LocalDateStringConverter.class)
    @Column(name = "date_read")
    private LocalDate dateRead;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    private AuthorEntity author;

    @ElementCollection
    @CollectionTable(name = "book_co_author", joinColumns = @JoinColumn(name = "book_id"))
    @OrderColumn(name = "position")
    @BatchSize(size = 100)
    @Column(name = "co_author", nullable = false)
    private List<String> coAuthors = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shelf_id")
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    private ShelfEntity shelf;

    @Column(name = "position_number")
    private Integer positionNumber;

    @Column(name = "depth_row", nullable = false)
    private int depthRow = 1;

    @Column(name = "has_photo", nullable = false)
    private boolean hasPhoto;

    @CreatedDate
    @NotAudited
    @Convert(converter = InstantStringConverter.class)
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @NotAudited
    @Convert(converter = InstantStringConverter.class)
    @Column(name = "modified_at")
    private Instant modifiedAt;

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getIsbn() {
        return isbn;
    }

    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getPublisher() {
        return publisher;
    }

    public void setPublisher(String publisher) {
        this.publisher = publisher;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Double getRating() {
        return rating;
    }

    public void setRating(Double rating) {
        this.rating = rating;
    }

    public LocalDate getPublicationDate() {
        return publicationDate;
    }

    public void setPublicationDate(LocalDate publicationDate) {
        this.publicationDate = publicationDate;
    }

    public List<String> getCoAuthors() {
        return coAuthors;
    }

    public void setCoAuthors(List<String> coAuthors) {
        this.coAuthors.clear();
        this.coAuthors.addAll(coAuthors);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getModifiedAt() {
        return modifiedAt;
    }

    public Integer getPages() {
        return pages;
    }

    public void setPages(Integer pages) {
        this.pages = pages;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public LocalDate getDateRead() {
        return dateRead;
    }

    public void setDateRead(LocalDate dateRead) {
        this.dateRead = dateRead;
    }

    public AuthorEntity getAuthor() {
        return author;
    }

    public void setAuthor(AuthorEntity author) {
        this.author = author;
    }

    public ShelfEntity getShelf() {
        return shelf;
    }

    public void setShelf(ShelfEntity shelf) {
        this.shelf = shelf;
    }

    public Integer getPositionNumber() {
        return positionNumber;
    }

    public void setPositionNumber(Integer positionNumber) {
        this.positionNumber = positionNumber;
    }

    public int getDepthRow() {
        return depthRow;
    }

    public void setDepthRow(int depthRow) {
        this.depthRow = depthRow;
    }

    public boolean isHasPhoto() {
        return hasPhoto;
    }

    public void setHasPhoto(boolean hasPhoto) {
        this.hasPhoto = hasPhoto;
    }
}
