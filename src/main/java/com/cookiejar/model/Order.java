package com.cookiejar.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String status;
    @Column(nullable = false)
    private Integer totalCents;
    @Column(nullable = false)
    private Instant createdAt = Instant.now();
    @ManyToOne
    @JoinColumn(name = "created_by_id")
    private Admin createdBy;
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<OrderItem> items = new ArrayList<>();
    public Order() {}
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getTotalCents() { return totalCents; }
    public void setTotalCents(Integer totalCents) { this.totalCents = totalCents; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Admin getCreatedBy() { return createdBy; }
    public void setCreatedBy(Admin createdBy) { this.createdBy = createdBy; }
    public List<OrderItem> getItems() { return items; }
    public void setItems(List<OrderItem> items) { this.items = items; }
}
