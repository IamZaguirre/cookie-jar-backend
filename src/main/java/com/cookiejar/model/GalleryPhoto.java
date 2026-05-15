package com.cookiejar.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "gallery_photos")
public class GalleryPhoto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String imageUrl;

    private String caption;

    @Column(nullable = false, columnDefinition = "integer NOT NULL DEFAULT 0")
    private Integer sortOrder = 0;

    @Column(nullable = false, columnDefinition = "boolean NOT NULL DEFAULT true")
    private Boolean active = true;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    public GalleryPhoto() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getCaption() { return caption; }
    public void setCaption(String caption) { this.caption = caption; }

    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
