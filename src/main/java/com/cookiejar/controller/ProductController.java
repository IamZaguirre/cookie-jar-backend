package com.cookiejar.controller;

import com.cookiejar.model.Product;
import com.cookiejar.repository.ProductRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {
    private final ProductRepository repository;
    public ProductController(ProductRepository repository) { this.repository = repository; }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Product p) {
        if (p.getName()==null || p.getPriceCents()==null) return ResponseEntity.badRequest().body("name and priceCents required");
        return ResponseEntity.status(201).body(repository.save(p));
    }

    @GetMapping
    public List<Product> list() { return repository.findAll(); }

    @GetMapping("/{id}")
    public ResponseEntity<Product> get(@PathVariable Long id) {
        return repository.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Product p) {
        return repository.findById(id).map(e -> {
            if (p.getName()!=null) e.setName(p.getName());
            if (p.getDescription()!=null) e.setDescription(p.getDescription());
            if (p.getPriceCents()!=null) e.setPriceCents(p.getPriceCents());
            if (p.getSku()!=null) e.setSku(p.getSku());
            if (p.getInventory()!=null) e.setInventory(p.getInventory());
            repository.save(e);
            return ResponseEntity.ok(e);
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!repository.existsById(id)) return ResponseEntity.notFound().build();
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
