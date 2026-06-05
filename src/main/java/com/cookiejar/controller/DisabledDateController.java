package com.cookiejar.controller;

import com.cookiejar.model.DisabledDate;
import com.cookiejar.repository.DisabledDateRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/disabled-dates")
public class DisabledDateController {

    private static final Set<String> VALID_TIMES = Set.of("10:00", "13:00", "16:00", "19:00", "21:00");

    private final DisabledDateRepository repository;

    public DisabledDateController(DisabledDateRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public ResponseEntity<List<DisabledDate>> getAll() {
        return ResponseEntity.ok(repository.findAll());
    }

    @PostMapping
    @SuppressWarnings("unchecked")
    public ResponseEntity<?> create(@RequestBody Map<String, Object> body) {
        String dateStr = (String) body.get("date");
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

        List<String> timesList = body.get("disabledTimes") != null
                ? (List<String>) body.get("disabledTimes")
                : List.of();
        for (String t : timesList) {
            if (!VALID_TIMES.contains(t)) {
                return ResponseEntity.badRequest().body("Invalid time slot: " + t);
            }
        }

        DisabledDate entity = new DisabledDate();
        entity.setDate(date);
        entity.setReason((String) body.get("reason"));
        entity.setPopupMessage((String) body.get("popupMessage"));
        entity.setDisabledTimes(new HashSet<>(timesList));
        return ResponseEntity.status(201).body(repository.save(entity));
    }

    @PatchMapping("/{id}")
    @SuppressWarnings("unchecked")
    public ResponseEntity<?> patch(@PathVariable("id") Long id, @RequestBody Map<String, Object> body) {
        return repository.findById(id).map(entity -> {
            if (body.containsKey("popupMessage")) {
                entity.setPopupMessage((String) body.get("popupMessage"));
            }
            if (body.containsKey("disabledTimes")) {
                List<String> times = (List<String>) body.get("disabledTimes");
                entity.setDisabledTimes(new HashSet<>(times));
            }
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
