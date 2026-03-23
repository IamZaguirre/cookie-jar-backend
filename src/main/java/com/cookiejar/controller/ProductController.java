package com.cookiejar.controller;

import com.cookiejar.model.Product;
import com.cookiejar.repository.ProductRepository;
import com.cookiejar.service.CloudinaryService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.*;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {
    private final ProductRepository repository;
    private final CloudinaryService cloudinaryService;

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
            System.out.println("Received productJson: " + productJson);
            ObjectMapper mapper = new ObjectMapper();
            Product p = mapper.readValue(productJson, Product.class);
            System.out.println("Parsed Product: name=" + p.getName() + ", priceCents=" + p.getPriceCents());
            if (p.getName()==null || p.getPriceCents()==null) return ResponseEntity.badRequest().body("name and priceCents required");
            if (p.getInStock() == null) p.setInStock(true);
            if (image != null && !image.isEmpty()) {
                String imageUrl = cloudinaryService.uploadImage(image);
                p.setImageUrl(imageUrl);
            }
            Product saved = repository.save(p);
            return ResponseEntity.status(201).body(saved);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Product creation failed: " + e.getMessage());
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
            System.out.println("Received productJson: " + productJson);
            ObjectMapper mapper = new ObjectMapper();
            Product p = mapper.readValue(productJson, Product.class);
            System.out.println("Parsed Product: name=" + p.getName() + ", priceCents=" + p.getPriceCents());
            return repository.findById(id).map(e -> {
                try {
                    if (p.getName()!=null) e.setName(p.getName());
                    if (p.getDescription()!=null) e.setDescription(p.getDescription());
                    if (p.getPriceCents()!=null) e.setPriceCents(p.getPriceCents());
                    if (p.getSku()!=null) e.setSku(p.getSku());
                    if (p.getInventory()!=null) e.setInventory(p.getInventory());
                    if (p.getInStock()!=null) e.setInStock(p.getInStock());
                    if (image != null && !image.isEmpty()) {
                        String oldImageUrl = e.getImageUrl();
                        String newImageUrl = cloudinaryService.uploadImage(image);
                        e.setImageUrl(newImageUrl);
                        cloudinaryService.deleteImage(oldImageUrl);
                    }
                    repository.save(e);
                    return ResponseEntity.ok(e);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    return ResponseEntity.internalServerError().body("Product update failed: " + ex.getMessage());
                }
            }).orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Product update failed: " + e.getMessage());
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
