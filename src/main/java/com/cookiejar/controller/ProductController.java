package com.cookiejar.controller;

import com.cookiejar.model.Product;
import com.cookiejar.repository.ProductRepository;
import com.cookiejar.service.CloudinaryService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.*;
import com.fasterxml.jackson.databind.ObjectMapper;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
public class ProductController {
    private final ProductRepository repository;
    private final CloudinaryService cloudinaryService;

    private static final Logger logger = LoggerFactory.getLogger(ProductController.class);

    public ProductController(ProductRepository repository, CloudinaryService cloudinaryService) {
        this.repository = repository;
        this.cloudinaryService = cloudinaryService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> create(
            @RequestPart("product") String productJson,
            @RequestPart(value = "image", required = false) MultipartFile image
    ) {
        try {
            logger.info("[CREATE PRODUCT] Received productJson: {}", productJson);
            ObjectMapper mapper = new ObjectMapper();
            Product p = mapper.readValue(productJson, Product.class);
            // Debug: log the type and value of inventory
            com.fasterxml.jackson.databind.JsonNode rootNode = mapper.readTree(productJson);
            com.fasterxml.jackson.databind.JsonNode inventoryNode = rootNode.get("inventory");
            logger.info(
                "[CREATE PRODUCT] Parsed Product: name={}, priceCents={}, inventory={}, rawInventoryType={}, rawInventoryValue={}",
                p.getName(),
                p.getPriceCents(),
                p.getInventory(),
                inventoryNode != null ? inventoryNode.getNodeType() : "null",
                inventoryNode != null ? inventoryNode.toString() : "null"
            );
            if (p.getName()==null || p.getPriceCents()==null) {
                logger.warn("[CREATE PRODUCT] name and priceCents required");
                return ResponseEntity.badRequest().body(Map.of("message", "name and priceCents required"));
            }
            if (p.getInventory() == null) p.setInventory(0);
            if (image != null && !image.isEmpty()) {
                String imageUrl = cloudinaryService.uploadImage(image);
                p.setImageUrl(imageUrl);
            }
            Product saved = repository.save(p);
            logger.info("[CREATE PRODUCT] Product saved with id={}", saved.getId());
            return ResponseEntity.status(201).body(saved);
        } catch (Exception e) {
            logger.error("[CREATE PRODUCT] Product creation failed", e);
            return ResponseEntity.internalServerError().body(Map.of("message", "Product creation failed: " + e.getMessage()));
        }
    }

    @GetMapping
    public List<Product> list() { return repository.findAll(); }

    @GetMapping("/{id}")
    public ResponseEntity<Product> get(@PathVariable("id") Long id) {
        return repository.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> update(
            @PathVariable("id") Long id,
            @RequestPart("product") String productJson,
            @RequestPart(value = "image", required = false) MultipartFile image
    ) {
        try {
            logger.info("[UPDATE PRODUCT] Received productJson: {} for id={}", productJson, id);
            ObjectMapper mapper = new ObjectMapper();
            Product p = mapper.readValue(productJson, Product.class);
            logger.info("[UPDATE PRODUCT] Parsed Product: name={}, priceCents={}, inventory={}", p.getName(), p.getPriceCents(), p.getInventory());
            return repository.findById(id)
                .<ResponseEntity<?>>map(e -> {
                    try {
                        if (p.getName()!=null) e.setName(p.getName());
                        if (p.getDescription()!=null) e.setDescription(p.getDescription());
                        if (p.getPriceCents()!=null) e.setPriceCents(p.getPriceCents());
                        if (p.getSku()!=null) e.setSku(p.getSku());
                        if (p.getInventory()!=null) e.setInventory(p.getInventory());
                        if (image != null && !image.isEmpty()) {
                            String oldImageUrl = e.getImageUrl();
                            String newImageUrl = cloudinaryService.uploadImage(image);
                            e.setImageUrl(newImageUrl);
                            cloudinaryService.deleteImage(oldImageUrl);
                        }
                        repository.save(e);
                        logger.info("[UPDATE PRODUCT] Product updated with id={}", e.getId());
                        return ResponseEntity.ok(e);
                    } catch (Exception ex) {
                        logger.error("[UPDATE PRODUCT] Product update failed for id=" + id, ex);
                        return ResponseEntity.internalServerError().body(Map.of("message", "Product update failed: " + ex.getMessage()));
                    }
                })
                .orElseGet(() -> {
                    logger.warn("[UPDATE PRODUCT] Product not found for id={}", id);
                    return ResponseEntity.status(404).body(Map.of("message", "Product not found"));
                });
        } catch (Exception e) {
            logger.error("[UPDATE PRODUCT] Product update failed for id=" + id, e);
            return ResponseEntity.internalServerError().body(Map.of("message", "Product update failed: " + e.getMessage()));
        }
    }

    @PostMapping(value = "/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadImage(@PathVariable("id") Long id, @RequestParam("image") MultipartFile image) {
        if (image.isEmpty()) {
            return ResponseEntity.badRequest().body("image file is required");
        }

        return repository.findById(id).map(product -> {
            try {
                String newImageUrl = cloudinaryService.uploadImage(image);
                cloudinaryService.deleteImage(product.getImageUrl());
                product.setImageUrl(newImageUrl);
                repository.save(product);
                return ResponseEntity.ok(product);
            } catch (IOException ex) {
                return ResponseEntity.internalServerError().body("failed to store image");
            }
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        return repository.findById(id).map(product -> {
            cloudinaryService.deleteImage(product.getImageUrl());
            repository.deleteById(id);
            return ResponseEntity.noContent().<Void>build();
        }).orElse(ResponseEntity.notFound().build());
    }
}
