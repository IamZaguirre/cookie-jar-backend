package com.cookiejar.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "products")
@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
public class Product {
        // Minimum hours before product can be delivered/edited
        @Column(name = "min_hours", nullable = true)
        private Integer minHours;
        @Column(name = "discount_percent", nullable = true)
        private Double discountPercent;
        @ElementCollection(fetch = FetchType.EAGER)
        @CollectionTable(name = "product_day_qty_limits", joinColumns = @JoinColumn(name = "product_id"))
        @MapKeyColumn(name = "day_of_week")
        @Column(name = "qty_limit")
        private Map<String, Integer> dayQtyLimits = new HashMap<>();
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @com.fasterxml.jackson.annotation.JsonManagedReference("product-boxflavors")
    @jakarta.persistence.OrderBy("sortOrder ASC")
    private List<BoxFlavor> boxFlavors = new ArrayList<>();
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String name;
    @Column(columnDefinition = "TEXT")
    private String description;
    @Column(nullable = false)
    private Integer priceCents;
    @Column(unique = true)
    private String sku;
    private String imageUrl;
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "product_images", joinColumns = @JoinColumn(name = "product_id"))
    @Column(name = "image_url")
    private List<String> imageUrls = new ArrayList<>();
    @Column(nullable = false)
    private Integer inventory;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @com.fasterxml.jackson.annotation.JsonManagedReference
    @jakarta.persistence.OrderBy("sortOrder ASC")
    private List<Variant> variants = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @com.fasterxml.jackson.annotation.JsonManagedReference("product-addons")
    private List<AddOn> addOns = new ArrayList<>();

    @Column(nullable = true)
    private String category;
    @Column(nullable = false, columnDefinition = "boolean NOT NULL DEFAULT true")
    private Boolean active = true;
    @Column(nullable = false)
    private Instant createdAt = Instant.now();
    @Column(nullable = false)
    private Instant updatedAt = Instant.now();
    @JsonIgnore
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL)
    private List<OrderItem> orderItems = new ArrayList<>();

    public Product() {}
    @PreUpdate
    public void onUpdate() { updatedAt = Instant.now(); }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Integer getPriceCents() { return priceCents; }
    public void setPriceCents(Integer priceCents) { this.priceCents = priceCents; }
    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public List<String> getImageUrls() { return imageUrls; }
    public void setImageUrls(List<String> imageUrls) { this.imageUrls = imageUrls; }
    public Integer getInventory() { return inventory; }
    public void setInventory(Integer inventory) { this.inventory = inventory; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public List<OrderItem> getOrderItems() { return orderItems; }
    public void setOrderItems(List<OrderItem> orderItems) { this.orderItems = orderItems; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public List<Variant> getVariants() { return variants; }
    public void setVariants(List<Variant> variants) {
        this.variants.clear();
        if (variants == null) {
            return;
        }
        for (Variant variant : variants) {
            variant.setProduct(this);
            this.variants.add(variant);
        }
    }

    public List<AddOn> getAddOns() { return addOns; }
    public void setAddOns(List<AddOn> addOns) {
        this.addOns.clear();
        if (addOns == null) {
            return;
        }
        for (AddOn addOn : addOns) {
            addOn.setProduct(this);
            this.addOns.add(addOn);
        }
    }

    public Integer getMinHours() { return minHours; }
    public void setMinHours(Integer minHours) { this.minHours = minHours; }

    public Double getDiscountPercent() { return discountPercent; }
    public void setDiscountPercent(Double discountPercent) { this.discountPercent = discountPercent; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }

    public Map<String, Integer> getDayQtyLimits() { return dayQtyLimits; }
    public void setDayQtyLimits(Map<String, Integer> dayQtyLimits) { this.dayQtyLimits = dayQtyLimits; }
    public List<BoxFlavor> getBoxFlavors() { return boxFlavors; }
    public void setBoxFlavors(List<BoxFlavor> boxFlavors) {
        this.boxFlavors.clear();
        if (boxFlavors == null) return;
        for (BoxFlavor bf : boxFlavors) { bf.setProduct(this); this.boxFlavors.add(bf); }
    }
}
