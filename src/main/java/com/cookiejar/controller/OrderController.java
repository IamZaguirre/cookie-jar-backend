package com.cookiejar.controller;

import com.cookiejar.model.Admin;
import com.cookiejar.model.Order;
import com.cookiejar.model.OrderItem;
import com.cookiejar.model.Product;
import com.cookiejar.repository.AdminRepository;
import com.cookiejar.repository.OrderRepository;
import com.cookiejar.repository.ProductRepository;
import com.cookiejar.service.CloudinaryService;
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
    private final AdminRepository adminRepository;
    private final CloudinaryService cloudinaryService;

    public OrderController(OrderRepository orderRepository, ProductRepository productRepository, AdminRepository adminRepository, CloudinaryService cloudinaryService) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.adminRepository = adminRepository;
        this.cloudinaryService = cloudinaryService;
    }

    @DeleteMapping("/all")
    public ResponseEntity<?> deleteAllOrders() {
        orderRepository.deleteAll();
        return ResponseEntity.ok("All orders deleted");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteOrder(@PathVariable("id") Long id) {
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
            Product p = productRepository.findById(productId).orElse(null);
            if (p == null) return ResponseEntity.badRequest().body("product not found");
            p.setInventory(p.getInventory()-qty);
            productRepository.save(p);
            OrderItem oi = new OrderItem(); oi.setProduct(p); oi.setQuantity(qty); oi.setUnitPrice(p.getPriceCents()); oi.setOrder(order);
            orderItems.add(oi);
            total += p.getPriceCents()*qty;
        }
        order.setTotalCents(total);
        order.setItems(orderItems);
        Order savedOrder = orderRepository.save(order);
        // Only return the id to the client to ensure a valid id is always present
        return ResponseEntity.status(201).body(java.util.Collections.singletonMap("id", savedOrder.getId()));
    }

    @GetMapping
    public List<Order> list() { return orderRepository.findAll(); }
    @GetMapping("/{id}")
        public ResponseEntity<Order> get(@PathVariable("id") Long id) {
        return orderRepository.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }
    @PatchMapping("/{id}/status")
        public ResponseEntity<?> status(@PathVariable("id") Long id,@RequestBody Map<String,String> body){
      String status=body.get("status"); if(status==null)return ResponseEntity.badRequest().body("status required");
      return orderRepository.findById(id).map(o->{ o.setStatus(status); return ResponseEntity.ok(orderRepository.save(o));}).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/proof-of-payment")
    public ResponseEntity<?> uploadProofOfPayment(@PathVariable("id") Long id, @RequestParam("image") MultipartFile image) {
        return orderRepository.findById(id).map(order -> {
            try {
                String imageUrl = cloudinaryService.uploadImage(image);
                order.setProofOfPaymentUrl(imageUrl);
                return ResponseEntity.ok(orderRepository.save(order));
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
