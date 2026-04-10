package com.cookiejar.model;

import jakarta.persistence.*;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
public class Order {
    private static final String ALPHANUM = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    @Id
    private String id;
    @Column(nullable = false)
    private String status;
    @Column(nullable = false)
    private Integer totalCents;
    @Column(nullable = false)
    private Instant createdAt = Instant.now();
    private Instant neededAt;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String proofOfPaymentUrl;
    @ElementCollection
    @CollectionTable(name = "order_proof_images", joinColumns = @JoinColumn(name = "order_id"))
    @Column(name = "image_url")
    @OrderColumn(name = "position")
    private List<String> proofOfPaymentUrls = new ArrayList<>();
    @ManyToOne
    @JoinColumn(name = "created_by_id")
    private Admin createdBy;
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<OrderItem> items = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            StringBuilder sb = new StringBuilder(10);
            for (int i = 0; i < 10; i++) {
                sb.append(ALPHANUM.charAt(RANDOM.nextInt(ALPHANUM.length())));
            }
            this.id = sb.toString();
        }
    }

    public Order() {}
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getTotalCents() { return totalCents; }
    public void setTotalCents(Integer totalCents) { this.totalCents = totalCents; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getNeededAt() { return neededAt; }
    public void setNeededAt(Instant neededAt) { this.neededAt = neededAt; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getProofOfPaymentUrl() { return proofOfPaymentUrl; }
    public void setProofOfPaymentUrl(String proofOfPaymentUrl) { this.proofOfPaymentUrl = proofOfPaymentUrl; }
    public List<String> getProofOfPaymentUrls() { return proofOfPaymentUrls; }
    public void setProofOfPaymentUrls(List<String> proofOfPaymentUrls) { this.proofOfPaymentUrls = proofOfPaymentUrls; }
    public Admin getCreatedBy() { return createdBy; }
    public void setCreatedBy(Admin createdBy) { this.createdBy = createdBy; }
    public List<OrderItem> getItems() { return items; }
    public void setItems(List<OrderItem> items) { this.items = items; }
}
