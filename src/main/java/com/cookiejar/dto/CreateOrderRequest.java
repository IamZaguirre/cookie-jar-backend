package com.cookiejar.dto;

import java.util.List;

public class CreateOrderRequest {

    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String neededAt;
    private Long createdById;
    private List<OrderItemRequest> items;

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getNeededAt() { return neededAt; }
    public void setNeededAt(String neededAt) { this.neededAt = neededAt; }

    public Long getCreatedById() { return createdById; }
    public void setCreatedById(Long createdById) { this.createdById = createdById; }

    public List<OrderItemRequest> getItems() { return items; }
    public void setItems(List<OrderItemRequest> items) { this.items = items; }

    public static class OrderItemRequest {
        private Long productId;
        private Long variantId;
        private Integer quantity;
        private List<SelectedAddOnRequest> selectedAddOns;
        private List<CardMessageRequest> cardMessages;
        private String cardMessage;

        public Long getProductId() { return productId; }
        public void setProductId(Long productId) { this.productId = productId; }

        public Long getVariantId() { return variantId; }
        public void setVariantId(Long variantId) { this.variantId = variantId; }

        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }

        public List<SelectedAddOnRequest> getSelectedAddOns() { return selectedAddOns; }
        public void setSelectedAddOns(List<SelectedAddOnRequest> selectedAddOns) { this.selectedAddOns = selectedAddOns; }

        public List<CardMessageRequest> getCardMessages() { return cardMessages; }
        public void setCardMessages(List<CardMessageRequest> cardMessages) { this.cardMessages = cardMessages; }

        public String getCardMessage() { return cardMessage; }
        public void setCardMessage(String cardMessage) { this.cardMessage = cardMessage; }
    }

    public static class SelectedAddOnRequest {
        private Long id;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
    }

    public static class CardMessageRequest {
        private Long addOnId;
        private String message;

        public Long getAddOnId() { return addOnId; }
        public void setAddOnId(Long addOnId) { this.addOnId = addOnId; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }
}
