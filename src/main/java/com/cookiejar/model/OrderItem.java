package com.cookiejar.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "order_items")
public class OrderItem {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;
    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id", nullable = true)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Variant variant;
    @Column(nullable = false)
    private Integer quantity;
    @Column(nullable = false)
    private Integer unitPrice;
    @Column(columnDefinition = "integer not null default 0")
    private Integer addOnTotalCents = 0;
    @Column
    private String variantName;
    @Column(columnDefinition = "text")
    private String cardMessage;
    @JsonIgnore
    @Column(columnDefinition = "text")
    private String cardMessagesJson;
    public OrderItem() {}
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Order getOrder() { return order; }
    public void setOrder(Order order) { this.order = order; }
    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }
    public Variant getVariant() { return variant; }
    public void setVariant(Variant variant) { this.variant = variant; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public Integer getUnitPrice() { return unitPrice; }
    public void setUnitPrice(Integer unitPrice) { this.unitPrice = unitPrice; }
    public Integer getAddOnTotalCents() { return addOnTotalCents != null ? addOnTotalCents : 0; }
    public void setAddOnTotalCents(Integer addOnTotalCents) { this.addOnTotalCents = addOnTotalCents != null ? addOnTotalCents : 0; }
    public String getVariantName() { return variantName; }
    public void setVariantName(String variantName) { this.variantName = variantName; }
    public String getCardMessage() { return cardMessage; }
    public void setCardMessage(String cardMessage) { this.cardMessage = cardMessage; }
    @Transient
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public List<CardMessageEntry> getCardMessages() {
        if (cardMessagesJson != null && !cardMessagesJson.isBlank()) {
            try {
                return OBJECT_MAPPER.readValue(cardMessagesJson, new TypeReference<List<CardMessageEntry>>() {});
            } catch (Exception ignored) {
            }
        }
        if (cardMessage != null && !cardMessage.isBlank()) {
            List<CardMessageEntry> legacyMessages = new ArrayList<>();
            legacyMessages.add(new CardMessageEntry(null, null, cardMessage));
            return legacyMessages;
        }
        return List.of();
    }
    public void setCardMessages(List<CardMessageEntry> cardMessages) {
        List<CardMessageEntry> sanitizedMessages = new ArrayList<>();
        if (cardMessages != null) {
            for (CardMessageEntry cardMessageEntry : cardMessages) {
                if (cardMessageEntry == null || cardMessageEntry.getMessage() == null) {
                    continue;
                }
                String message = cardMessageEntry.getMessage().trim();
                if (message.isBlank()) {
                    continue;
                }
                sanitizedMessages.add(new CardMessageEntry(
                    cardMessageEntry.getAddOnId(),
                    cardMessageEntry.getAddOnName(),
                    message
                ));
            }
        }

        if (sanitizedMessages.isEmpty()) {
            this.cardMessagesJson = null;
            this.cardMessage = null;
            return;
        }

        try {
            this.cardMessagesJson = OBJECT_MAPPER.writeValueAsString(sanitizedMessages);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize card messages", e);
        }

        this.cardMessage = sanitizedMessages.size() == 1
                ? sanitizedMessages.get(0).getMessage()
                : null;
    }

    public static class CardMessageEntry {
        private Long addOnId;
        private String addOnName;
        private String message;

        public CardMessageEntry() {}

        public CardMessageEntry(Long addOnId, String addOnName, String message) {
            this.addOnId = addOnId;
            this.addOnName = addOnName;
            this.message = message;
        }

        public Long getAddOnId() { return addOnId; }
        public void setAddOnId(Long addOnId) { this.addOnId = addOnId; }
        public String getAddOnName() { return addOnName; }
        public void setAddOnName(String addOnName) { this.addOnName = addOnName; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }
}
