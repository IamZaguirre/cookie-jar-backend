package com.cookiejar.model;

import jakarta.persistence.*;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "box_flavors")
@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
public class BoxFlavor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "sort_order", nullable = false, columnDefinition = "integer NOT NULL DEFAULT 0")
    private Integer sortOrder = 0;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "box_flavor_day_qty_limits", joinColumns = @JoinColumn(name = "box_flavor_id"))
    @MapKeyColumn(name = "day_of_week")
    @Column(name = "qty_limit")
    private Map<String, Integer> dayQtyLimits = new HashMap<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    @com.fasterxml.jackson.annotation.JsonBackReference("product-boxflavors")
    private Product product;

    public BoxFlavor() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public Map<String, Integer> getDayQtyLimits() { return dayQtyLimits; }
    public void setDayQtyLimits(Map<String, Integer> dayQtyLimits) { this.dayQtyLimits = dayQtyLimits; }
    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }
}
