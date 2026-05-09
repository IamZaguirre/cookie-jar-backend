package com.cookiejar.controller;

import com.cookiejar.model.DisabledDate;
import com.cookiejar.repository.DisabledDateRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/disabled-dates")
public class DisabledDateController {

    private final DisabledDateRepository repository;

    public DisabledDateController(DisabledDateRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public ResponseEntity<List<DisabledDate>> getAll() {
        return ResponseEntity.ok(repository.findAll());
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Map<String, String> body) {
        String dateStr = body.get("date");
        if (dateStr == null || dateStr.isBlank()) {
            return ResponseEntity.badRequest().body("date is required (YYYY-MM-DD)");
        }
        LocalDate date;
        try {
            date = LocalDate.parse(dateStr);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("date must be in YYYY-MM-DD format");
        }
        if (repository.existsByDate(date)) {
            return ResponseEntity.status(409).body("date already disabled");
        }
        DisabledDate entity = new DisabledDate();
        entity.setDate(date);
        entity.setReason(body.get("reason"));
        entity.setPopupMessage(body.get("popupMessage"));
        return ResponseEntity.status(201).body(repository.save(entity));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<?> updatePopupMessage(@PathVariable("id") Long id, @RequestBody Map<String, String> body) {
        return repository.findById(id).map(entity -> {
            entity.setPopupMessage(body.getOrDefault("popupMessage", null));
            return ResponseEntity.ok(repository.save(entity));
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        if (!repository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
