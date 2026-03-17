package com.cookiejar.controller;

import com.cookiejar.model.Product;
import com.cookiejar.repository.ProductRepository;
import com.cookiejar.service.CloudinaryService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.*;

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

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Product p) {
        if (p.getName()==null || p.getPriceCents()==null) return ResponseEntity.badRequest().body("name and priceCents required");
        if (p.getInStock() == null) p.setInStock(true);
        return ResponseEntity.status(201).body(repository.save(p));
    }

    @GetMapping
    public List<Product> list() { return repository.findAll(); }

    @GetMapping("/{id}")
    public ResponseEntity<Product> get(@PathVariable("id") Long id) {
        return repository.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable("id") Long id, @RequestBody Product p) {
        return repository.findById(id).map(e -> {
            if (p.getName()!=null) e.setName(p.getName());
            if (p.getDescription()!=null) e.setDescription(p.getDescription());
            if (p.getPriceCents()!=null) e.setPriceCents(p.getPriceCents());
            if (p.getSku()!=null) e.setSku(p.getSku());
            if (p.getImageUrl()!=null) e.setImageUrl(p.getImageUrl());
            if (p.getInventory()!=null) e.setInventory(p.getInventory());
            if (p.getInStock()!=null) e.setInStock(p.getInStock());
            repository.save(e);
            return ResponseEntity.ok(e);
        }).orElse(ResponseEntity.notFound().build());
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
