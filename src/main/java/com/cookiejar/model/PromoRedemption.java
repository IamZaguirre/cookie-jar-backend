package com.cookiejar.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(
    name = "promo_redemptions",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_promo_redemption_email", columnNames = {"promo_id", "email"}),
        @UniqueConstraint(name = "uk_promo_redemption_phone", columnNames = {"promo_id", "phone"})
    }
)
public class PromoRedemption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "promo_id", nullable = false)
    private Long promoId;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String phone;

    @Column(unique = true)
    private String redemptionKey;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    public PromoRedemption() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getPromoId() { return promoId; }
    public void setPromoId(Long promoId) { this.promoId = promoId; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getRedemptionKey() { return redemptionKey; }
    public void setRedemptionKey(String redemptionKey) { this.redemptionKey = redemptionKey; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
