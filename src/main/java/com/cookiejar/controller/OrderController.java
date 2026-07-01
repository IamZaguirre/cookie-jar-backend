package com.cookiejar.controller;

import com.cookiejar.dto.CreateOrderRequest;
import com.cookiejar.model.AddOn;
import com.cookiejar.model.Admin;
import com.cookiejar.model.Order;
import com.cookiejar.model.OrderItem;
import com.cookiejar.model.Product;
import com.cookiejar.model.Variant;
import com.cookiejar.repository.AddOnRepository;
import com.cookiejar.repository.AdminRepository;
import com.cookiejar.repository.OrderRepository;
import com.cookiejar.repository.ProductRepository;
import com.cookiejar.repository.VariantRepository;
import com.cookiejar.service.CloudinaryService;
import com.cookiejar.service.EmailService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final VariantRepository variantRepository;
    private final AddOnRepository addOnRepository;
    private final AdminRepository adminRepository;
    private final CloudinaryService cloudinaryService;
    private final EmailService emailService;

    public OrderController(OrderRepository orderRepository, ProductRepository productRepository, VariantRepository variantRepository, AddOnRepository addOnRepository, AdminRepository adminRepository, CloudinaryService cloudinaryService, EmailService emailService) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.variantRepository = variantRepository;
        this.addOnRepository = addOnRepository;
        this.adminRepository = adminRepository;
        this.cloudinaryService = cloudinaryService;
        this.emailService = emailService;
    }

    @DeleteMapping("/all")
    public ResponseEntity<?> deleteAllOrders() {
        orderRepository.deleteAll();
        return ResponseEntity.ok("All orders deleted");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteOrder(@PathVariable("id") String id) {
        return orderRepository.findById(id).map(order -> {
            if (order.getProofOfPaymentUrl() != null) {
                cloudinaryService.deleteImage(order.getProofOfPaymentUrl());
            }
            orderRepository.deleteById(id);
            return ResponseEntity.noContent().<Void>build();
        }).orElse(ResponseEntity.notFound().build());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("message", "Validation failed");
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.putIfAbsent(fe.getField(), fe.getDefaultMessage());
        }
        body.put("errors", fieldErrors);
        return ResponseEntity.badRequest().body(body);
    }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody CreateOrderRequest body) {
        System.out.println("[Order] POST /api/orders received: " + body);
        List<CreateOrderRequest.OrderItemRequest> items = body.getItems();
        Long createdById = body.getCreatedById();
        String neededAtValue = body.getNeededAt() != null ? body.getNeededAt().trim() : null;
        if (items == null || items.isEmpty()) return ResponseEntity.badRequest().body("items required");
        Order order = new Order();
        order.setStatus("pending");
        order.setTotalCents(0);
        order.setFirstName(body.getFirstName());
        order.setLastName(body.getLastName());
        order.setEmail(body.getEmail());
        order.setPhone(body.getPhone());
        if (neededAtValue != null && !neededAtValue.isEmpty()) {
            try {
                order.setNeededAt(Instant.parse(neededAtValue));
            } catch (Exception ex) {
                return ResponseEntity.badRequest().body("neededAt must be a valid ISO-8601 datetime");
            }
        }
        if (createdById != null) {
            Admin admin = adminRepository.findById(createdById).orElse(null);
            order.setCreatedBy(admin);
        }
        List<OrderItem> orderItems = new ArrayList<>();
        int total=0;
        for (CreateOrderRequest.OrderItemRequest i : items) {
            Long productId = i.getProductId();
            int qty = i.getQuantity();
            Long variantId = i.getVariantId();
            List<CreateOrderRequest.SelectedAddOnRequest> selectedAddOns = i.getSelectedAddOns() != null ? i.getSelectedAddOns() : List.of();
            System.out.println("[Order] Processing item: productId=" + productId + " variantId=" + variantId + " qty=" + qty);
            Product p = productRepository.findById(productId).orElse(null);
            if (p == null) { System.out.println("[Order] Product not found: " + productId); return ResponseEntity.badRequest().body("One of your selected products is no longer available. Please remove it from your order and try again."); }
            int unitPrice;
            String variantName = null;
            Variant selectedVariant = null;
            if (variantId != null) {
                Variant v = variantRepository.findById(variantId).orElse(null);
                if (v == null) { System.out.println("[Order] Variant not found: " + variantId); return ResponseEntity.badRequest().body("A selected option for \"" + p.getName() + "\" is no longer available. Please go back and reselect your options."); }
                selectedVariant = v;
                // Validate available days + qty limit (variant-level, fallback to product-level)
                Map<String, Integer> activeLimits =
                        (v.getDayQtyLimits() != null && !v.getDayQtyLimits().isEmpty())
                                ? v.getDayQtyLimits()
                                : (p.getDayQtyLimits() != null && !p.getDayQtyLimits().isEmpty() ? p.getDayQtyLimits() : null);
                if (activeLimits != null && order.getNeededAt() != null) {
                    java.time.ZoneId zone = java.time.ZoneId.of("Asia/Manila");
                    String dayOfWeek = order.getNeededAt().atZone(zone).getDayOfWeek().name();
                    if (!activeLimits.containsKey(dayOfWeek)) {
                        return ResponseEntity.badRequest().body("\"" + p.getName() + " — " + v.getName() + "\" is not available on that day. Please choose a different pick-up date.");
                    }
                    Integer activeLimit = activeLimits.get(dayOfWeek);
                    java.time.LocalDate pickupDate = order.getNeededAt().atZone(zone).toLocalDate();
                    Instant dayStart = pickupDate.atStartOfDay(zone).toInstant();
                    Instant dayEnd = pickupDate.plusDays(1).atStartOfDay(zone).toInstant();
                    int alreadyOrdered = (v.getDayQtyLimits() != null && !v.getDayQtyLimits().isEmpty())
                            ? orderRepository.sumQtyForVariantOnDay(v.getId(), dayStart, dayEnd)
                            : orderRepository.sumQtyForProductOnDay(p.getId(), dayStart, dayEnd);
                    if (alreadyOrdered + qty > activeLimit) {
                        int remaining = Math.max(0, activeLimit - alreadyOrdered);
                        String label = "\"" + p.getName() + " — " + v.getName() + "\"";
                        String msg = remaining == 0
                                ? label + " has reached its daily limit for that day. Please choose a different pick-up date."
                                : label + " only has " + remaining + " slot(s) left for that day. You requested " + qty + ".";
                        return ResponseEntity.badRequest().body(msg);
                    }
                }
                double discount = (v.getDiscountPercent() != null && v.getDiscountPercent() > 0)
                        ? v.getDiscountPercent()
                        : (p.getDiscountPercent() != null ? p.getDiscountPercent() : 0);
                unitPrice = discount > 0
                        ? (int) Math.round(v.getPriceCents() * (1 - discount / 100.0))
                        : v.getPriceCents();
                variantName = v.getName();
                if (v.getInventory() < qty) {
                    int available = v.getInventory();
                    String availableMsg = available == 0 ? "is out of stock" : "only has " + available + " left in stock";
                    return ResponseEntity.badRequest().body("\"" + p.getName() + " — " + variantName + "\" " + availableMsg + ". You requested " + qty + ".");
                }
                v.setInventory(v.getInventory() - qty);
                variantRepository.save(v);
            } else {
                // Validate available days (product-level)
                if (p.getDayQtyLimits() != null && !p.getDayQtyLimits().isEmpty() && order.getNeededAt() != null) {
                    java.time.ZoneId zone = java.time.ZoneId.of("Asia/Manila");
                    String dayOfWeek = order.getNeededAt().atZone(zone).getDayOfWeek().name();
                    if (!p.getDayQtyLimits().containsKey(dayOfWeek)) {
                        return ResponseEntity.badRequest().body("\"" + p.getName() + "\" is not available on that day. Please choose a different pick-up date.");
                    }
                    Integer activeLimit = p.getDayQtyLimits().get(dayOfWeek);
                    java.time.LocalDate pickupDate = order.getNeededAt().atZone(zone).toLocalDate();
                    Instant dayStart = pickupDate.atStartOfDay(zone).toInstant();
                    Instant dayEnd = pickupDate.plusDays(1).atStartOfDay(zone).toInstant();
                    int alreadyOrdered = orderRepository.sumQtyForProductOnDay(p.getId(), dayStart, dayEnd);
                    if (alreadyOrdered + qty > activeLimit) {
                        int remaining = Math.max(0, activeLimit - alreadyOrdered);
                        String msg = remaining == 0
                                ? "\"" + p.getName() + "\" has reached its daily limit for that day. Please choose a different pick-up date."
                                : "\"" + p.getName() + "\" only has " + remaining + " slot(s) left for that day. You requested " + qty + ".";
                        return ResponseEntity.badRequest().body(msg);
                    }
                }
                unitPrice = p.getPriceCents();
                if (p.getDiscountPercent() != null && p.getDiscountPercent() > 0) {
                    unitPrice = (int) Math.round(p.getPriceCents() * (1 - p.getDiscountPercent() / 100.0));
                }
                if (p.getInventory() < qty) {
                    int available = p.getInventory();
                    String availableMsg = available == 0 ? "is out of stock" : "only has " + available + " left in stock";
                    return ResponseEntity.badRequest().body("\"" + p.getName() + "\" " + availableMsg + ". You requested " + qty + ".");
                }
                p.setInventory(p.getInventory() - qty);
                productRepository.save(p);
            }
            List<String> addOnNames = new ArrayList<>();
            Map<Long, String> selectedAddOnNamesById = new HashMap<>();
            int addOnTotalCents = 0;
            for (CreateOrderRequest.SelectedAddOnRequest selectedAddOn : selectedAddOns) {
                Long addOnId = selectedAddOn.getId();
                if (addOnId == null) {
                    return ResponseEntity.badRequest().body("selected add-on id required");
                }
                AddOn addOn = addOnRepository.findById(addOnId).orElse(null);
                if (addOn == null || addOn.getProduct() == null || !productId.equals(addOn.getProduct().getId())) {
                    return ResponseEntity.badRequest().body("invalid add-on selected");
                }
                addOnTotalCents += addOn.getPriceCents();
                addOnNames.add(addOn.getName());
                selectedAddOnNamesById.put(addOnId, addOn.getName());
            }
            if (!addOnNames.isEmpty()) {
                String addOnSummary = "Add-ons: " + String.join(", ", addOnNames);
                variantName = (variantName == null || variantName.isBlank())
                        ? addOnSummary
                        : variantName + " + " + addOnSummary;
            }
            System.out.println("[Order] Item ready: name=" + p.getName() + " variantName=" + variantName + " unitPrice=" + unitPrice + " addOnTotalCents=" + addOnTotalCents);
            OrderItem oi = new OrderItem();
            oi.setProduct(p);
            oi.setVariant(selectedVariant);
            oi.setQuantity(qty);
            oi.setUnitPrice(unitPrice);
            oi.setAddOnTotalCents(addOnTotalCents);
            oi.setVariantName(variantName);
            List<OrderItem.CardMessageEntry> cardMessages = new ArrayList<>();
            if (i.getCardMessages() != null) {
                for (CreateOrderRequest.CardMessageRequest cardMessageReq : i.getCardMessages()) {
                    Long addOnId = cardMessageReq.getAddOnId();
                    if (addOnId == null || !selectedAddOnNamesById.containsKey(addOnId)) {
                        return ResponseEntity.badRequest().body("card message add-on must match a selected add-on");
                    }
                    String message = cardMessageReq.getMessage() != null ? cardMessageReq.getMessage().trim() : "";
                    if (message.isBlank()) {
                        continue;
                    }
                    cardMessages.add(new OrderItem.CardMessageEntry(
                            addOnId,
                            selectedAddOnNamesById.get(addOnId),
                            message
                    ));
                }
            } else if (i.getCardMessage() != null && !i.getCardMessage().isBlank()) {
                cardMessages.add(new OrderItem.CardMessageEntry(null, null, i.getCardMessage().trim()));
            }
            oi.setCardMessages(cardMessages);
            oi.setOrder(order);
            orderItems.add(oi);
            total += unitPrice * qty + addOnTotalCents;
        }
        order.setTotalCents(total);
        order.setItems(orderItems);
        System.out.println("[Order] Saving order, total=" + total + " items=" + orderItems.size());
        Order savedOrder;
        try {
            savedOrder = orderRepository.save(order);
        } catch (Exception e) {
            System.err.println("[Order] Failed to save order: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Failed to save order: " + e.getMessage());
        }
        System.out.println("[Order] Saved order id=" + savedOrder.getId());
        try {
            emailService.sendNewOrderNotification(savedOrder);
            emailService.sendOrderConfirmationToCustomer(savedOrder);
        } catch (Exception e) {
            System.err.println("[Email] Failed to send order notification for " + savedOrder.getId() + ": " + e.getMessage());
        }
        return ResponseEntity.status(201).body(java.util.Collections.singletonMap("id", savedOrder.getId()));
    }

    @GetMapping
    public List<Order> list() { return orderRepository.findAll(); }
    @GetMapping("/{id}")
        public ResponseEntity<Order> get(@PathVariable("id") String id) {
        return orderRepository.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }
    @PatchMapping("/{id}/status")
        public ResponseEntity<?> status(@PathVariable("id") String id,@RequestBody Map<String,String> body){
      String status=body.get("status"); if(status==null)return ResponseEntity.badRequest().body("status required");
      return orderRepository.findById(id).map(o -> {
          o.setStatus(status);
          Order updated = orderRepository.save(o);
          try {
              emailService.sendStatusUpdateNotification(updated);
          } catch (Exception e) {
              System.err.println("[Email] Failed to send status update for " + id + ": " + e.getMessage());
          }
          return ResponseEntity.ok(updated);
      }).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/repayment-request")
    public ResponseEntity<?> repaymentRequest(@PathVariable("id") String id, @RequestBody Map<String, String> body) {
        String message = body.get("message");
        if (message == null || message.isBlank()) return ResponseEntity.badRequest().body("message required");
        return orderRepository.findById(id).map(order -> {
            if (order.getEmail() == null || order.getEmail().isBlank()) {
                return ResponseEntity.badRequest().body("Order has no customer email.");
            }
            try {
                emailService.sendRepaymentRequestEmail(order, message);
                return ResponseEntity.ok(java.util.Collections.singletonMap("sent", true));
            } catch (Exception e) {
                return ResponseEntity.internalServerError().body("Failed to send repayment request email.");
            }
        }).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/proof-of-payment")
    public ResponseEntity<?> uploadProofOfPayment(@PathVariable("id") String id, @RequestParam("image") MultipartFile image) {
        return orderRepository.findById(id).map(order -> {
            try {
                boolean isResubmission = order.getProofOfPaymentUrl() != null;
                String imageUrl = cloudinaryService.uploadImage(image);
                // Migrate: if there was an original proof but the list is empty, add it first
                if (isResubmission && order.getProofOfPaymentUrls().isEmpty()) {
                    order.getProofOfPaymentUrls().add(order.getProofOfPaymentUrl());
                }
                order.setProofOfPaymentUrl(imageUrl);
                order.getProofOfPaymentUrls().add(imageUrl);
                Order savedOrder = orderRepository.save(order);
                if (isResubmission) {
                    emailService.sendPaymentResubmissionNotification(savedOrder);
                }
                return ResponseEntity.ok(savedOrder);
            } catch (Exception e) {
                StringBuilder sb = new StringBuilder();
                sb.append("Failed to upload proof of payment: ").append(e.getMessage()).append("\n");
                for (StackTraceElement ste : e.getStackTrace()) {
                    sb.append(ste.toString()).append("\n");
                }
                return ResponseEntity.internalServerError().body(sb.toString());
            }
        }).orElse(ResponseEntity.notFound().build());
    }
}
