package com.cookiejar.model;

import jakarta.persistence.*;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "variants")
@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
public class Variant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Integer inventory;

    @Column(nullable = false)
    private Integer priceCents;

    @Column(name = "discount_percent", nullable = true)
    private Double discountPercent;

    @Column(name = "sort_order", nullable = false, columnDefinition = "integer NOT NULL DEFAULT 0")
    private Integer sortOrder = 0;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "variant_day_qty_limits", joinColumns = @JoinColumn(name = "variant_id"))
    @MapKeyColumn(name = "day_of_week")
    @Column(name = "qty_limit")
    private Map<String, Integer> dayQtyLimits = new HashMap<>();

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
    public Double getDiscountPercent() { return discountPercent; }
    public void setDiscountPercent(Double discountPercent) { this.discountPercent = discountPercent; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public Map<String, Integer> getDayQtyLimits() { return dayQtyLimits; }
    public void setDayQtyLimits(Map<String, Integer> dayQtyLimits) { this.dayQtyLimits = dayQtyLimits; }
    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }
}