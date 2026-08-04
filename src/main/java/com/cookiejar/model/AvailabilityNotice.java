package com.cookiejar.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "availability_notice")
public class AvailabilityNotice {

    public static final long SINGLETON_ID = 1L;

    @Id
    private Long id = SINGLETON_ID;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    public AvailabilityNotice() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    @PrePersist
    @PreUpdate
    public void touch() {
        updatedAt = Instant.now();
        if (id == null) id = SINGLETON_ID;
    }
}
