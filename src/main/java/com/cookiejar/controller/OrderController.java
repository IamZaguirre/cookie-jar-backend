package com.cookiejar.controller;

import com.cookiejar.model.Admin;
import com.cookiejar.model.Order;
import com.cookiejar.model.OrderItem;
import com.cookiejar.model.Product;
import com.cookiejar.model.Variant;
import com.cookiejar.repository.AdminRepository;
import com.cookiejar.repository.OrderRepository;
import com.cookiejar.repository.ProductRepository;
import com.cookiejar.repository.VariantRepository;
import com.cookiejar.service.CloudinaryService;
import com.cookiejar.service.EmailService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final VariantRepository variantRepository;
    private final AdminRepository adminRepository;
    private final CloudinaryService cloudinaryService;
    private final EmailService emailService;

    public OrderController(OrderRepository orderRepository, ProductRepository productRepository, VariantRepository variantRepository, AdminRepository adminRepository, CloudinaryService cloudinaryService, EmailService emailService) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.variantRepository = variantRepository;
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

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Map<String, Object> body) {
        List<Map<String,Object>> items = (List<Map<String,Object>>) body.get("items");
        Number createdById = (Number) body.get("createdById");
        String neededAtValue = body.get("neededAt") instanceof String ? ((String) body.get("neededAt")).trim() : null;
        if (items == null || items.isEmpty()) return ResponseEntity.badRequest().body("items required");
        Order order = new Order();
        order.setStatus("pending");
        order.setTotalCents(0);
        order.setFirstName((String) body.get("firstName"));
        order.setLastName((String) body.get("lastName"));
        order.setEmail((String) body.get("email"));
        order.setPhone((String) body.get("phone"));
        if (neededAtValue != null && !neededAtValue.isEmpty()) {
            try {
                order.setNeededAt(Instant.parse(neededAtValue));
            } catch (Exception ex) {
                return ResponseEntity.badRequest().body("neededAt must be a valid ISO-8601 datetime");
            }
        }
        if (createdById != null) {
            Admin admin = adminRepository.findById(createdById.longValue()).orElse(null);
            order.setCreatedBy(admin);
        }
        List<OrderItem> orderItems = new ArrayList<>();
        int total=0;
        for (Map<String,Object> i : items) {
            Long productId = ((Number)i.get("productId")).longValue();
            int qty = ((Number)i.get("quantity")).intValue();
            Long variantId = i.get("variantId") instanceof Number ? ((Number)i.get("variantId")).longValue() : null;
            Product p = productRepository.findById(productId).orElse(null);
            if (p == null) return ResponseEntity.badRequest().body("product not found");
            int unitPrice;
            String variantName = null;
            if (variantId != null) {
                Variant v = variantRepository.findById(variantId).orElse(null);
                if (v == null) return ResponseEntity.badRequest().body("variant not found");
                double discount = (v.getDiscountPercent() != null && v.getDiscountPercent() > 0)
                        ? v.getDiscountPercent()
                        : (p.getDiscountPercent() != null ? p.getDiscountPercent() : 0);
                unitPrice = discount > 0
                        ? (int) Math.round(v.getPriceCents() * (1 - discount / 100.0))
                        : v.getPriceCents();
                variantName = v.getName();
                v.setInventory(v.getInventory() - qty);
                variantRepository.save(v);
            } else {
                unitPrice = p.getPriceCents();
                if (p.getDiscountPercent() != null && p.getDiscountPercent() > 0) {
                    unitPrice = (int) Math.round(p.getPriceCents() * (1 - p.getDiscountPercent() / 100.0));
                }
                p.setInventory(p.getInventory() - qty);
                productRepository.save(p);
            }
            OrderItem oi = new OrderItem();
            oi.setProduct(p);
            oi.setQuantity(qty);
            oi.setUnitPrice(unitPrice);
            oi.setVariantName(variantName);
            oi.setOrder(order);
            orderItems.add(oi);
            total += unitPrice*qty;
        }
        order.setTotalCents(total);
        order.setItems(orderItems);
        Order savedOrder = orderRepository.save(order);
        try {
            emailService.sendNewOrderNotification(savedOrder);
            emailService.sendOrderConfirmationToCustomer(savedOrder);
        } catch (Exception e) {
            // Rollback: delete order and restore inventory
            orderRepository.deleteById(savedOrder.getId());
            for (OrderItem oi : orderItems) {
                Product p = oi.getProduct();
                p.setInventory(p.getInventory() + oi.getQuantity());
                productRepository.save(p);
            }
            return ResponseEntity.internalServerError().body("Order not placed: failed to send notification email.");
        }
        // Only return the id to the client to ensure a valid id is always present
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
          try {
              emailService.sendStatusUpdateNotification(o);
          } catch (Exception e) {
              return ResponseEntity.internalServerError().body("Status not updated: failed to send notification email.");
          }
          return ResponseEntity.ok(orderRepository.save(o));
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
