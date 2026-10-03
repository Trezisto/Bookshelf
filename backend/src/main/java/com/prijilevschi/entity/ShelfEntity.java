package com.prijilevschi.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "shelf", uniqueConstraints = @UniqueConstraint(columnNames = {"location", "row_num"}))
public class ShelfEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", columnDefinition = "integer") // SQLite rowid alias
    private Long id;

    @Column(name = "location", nullable = false)
    private String location;

    @Column(name = "row_num", nullable = false)
    private int rowNum;

    @Enumerated(EnumType.STRING)
    @Column(name = "orientation", nullable = false)
    private ShelfOrientation orientation = ShelfOrientation.VERTICAL;

    public Long getId() {
        return id;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public int getRowNum() {
        return rowNum;
    }

    public void setRowNum(int rowNum) {
        this.rowNum = rowNum;
    }

    public ShelfOrientation getOrientation() {
        return orientation;
    }

    public void setOrientation(ShelfOrientation orientation) {
        this.orientation = orientation;
    }
}
