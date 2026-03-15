package com.cookiejar.controller;

import com.cookiejar.model.Admin;
import com.cookiejar.repository.AdminRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "http://localhost:3001")
public class AdminController {
    private final AdminRepository repository;
    public AdminController(AdminRepository repository) { this.repository = repository; }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Admin admin) {
        if (admin.getEmail() == null || admin.getPassword() == null) return ResponseEntity.badRequest().body("email and password required");
        if (repository.findByEmail(admin.getEmail()).isPresent()) return ResponseEntity.status(409).body("email already exists");
        Admin saved = repository.save(admin);
        saved.setPassword(null);
        return ResponseEntity.status(201).body(saved);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable Long id) {
        Optional<Admin> a = repository.findById(id);
        if (a.isEmpty()) return ResponseEntity.notFound().build();
        Admin admin = a.get();
        admin.setPassword(null);
        return ResponseEntity.ok(admin);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Admin admin) {
        return repository.findById(id).map(e -> {
            if (admin.getName() != null) e.setName(admin.getName());
            if (admin.getPassword() != null) e.setPassword(admin.getPassword());
            repository.save(e);
            e.setPassword(null);
            return ResponseEntity.ok(e);
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!repository.existsById(id)) return ResponseEntity.notFound().build();
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Admin admin) {
        if (admin.getEmail() == null || admin.getPassword() == null) {
            return ResponseEntity.badRequest().body("email and password required");
        }
        Optional<Admin> found = repository.findByEmail(admin.getEmail());
        if (found.isEmpty() || !found.get().getPassword().equals(admin.getPassword())) {
            return ResponseEntity.status(401).body("invalid credentials");
        }
        Admin result = found.get();
        result.setPassword(null);
        return ResponseEntity.ok(result);
    }
}
