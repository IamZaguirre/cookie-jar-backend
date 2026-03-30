package com.cookiejar.model;

import jakarta.persistence.*;

@Entity
@Table(name = "variants")
public class Variant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name; // e.g. "1pc", "4pcs"

    @Column(nullable = false)
    private Integer inventory;

    @Column(nullable = false)
    private Integer priceCents;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    @com.fasterxml.jackson.annotation.JsonBackReference
    private Product product;

    public Variant() {}

    public Variant(String name, Integer inventory, Integer priceCents, Product product) {
        this.name = name;
        this.inventory = inventory;
        this.priceCents = priceCents;
        this.product = product;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getInventory() { return inventory; }
    public void setInventory(Integer inventory) { this.inventory = inventory; }
    public Integer getPriceCents() { return priceCents; }
    public void setPriceCents(Integer priceCents) { this.priceCents = priceCents; }
    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }
}