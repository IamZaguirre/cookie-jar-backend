package com.cookiejar.controller;

import com.cookiejar.model.SiteAdvisory;
import com.cookiejar.repository.SiteAdvisoryRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/site-advisory")
public class SiteAdvisoryController {

    private final SiteAdvisoryRepository repository;

    public SiteAdvisoryController(SiteAdvisoryRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public ResponseEntity<SiteAdvisory> getActive() {
        return repository
                .findById(SiteAdvisory.SINGLETON_ID)
                .filter(advisory -> Boolean.TRUE.equals(advisory.getActive()))
                .filter(advisory -> advisory.getMessage() != null && !advisory.getMessage().trim().isEmpty())
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping("/admin")
    public ResponseEntity<SiteAdvisory> getForAdmin() {
        SiteAdvisory advisory = repository
                .findById(SiteAdvisory.SINGLETON_ID)
                .orElseGet(SiteAdvisory::new);

        advisory.setId(SiteAdvisory.SINGLETON_ID);
        if (advisory.getActive() == null) {
            advisory.setActive(false);
        }

        return ResponseEntity.ok(advisory);
    }

    @PutMapping("/admin")
    public ResponseEntity<?> upsert(@RequestBody SiteAdvisory body) {
        String nextMessage = body.getMessage() == null ? "" : body.getMessage().trim();
        if (Boolean.TRUE.equals(body.getActive()) && nextMessage.isEmpty()) {
            return ResponseEntity.badRequest().body("Active advisory requires a message.");
        }

        SiteAdvisory advisory = repository
                .findById(SiteAdvisory.SINGLETON_ID)
                .orElseGet(SiteAdvisory::new);

        advisory.setId(SiteAdvisory.SINGLETON_ID);
        advisory.setMessage(nextMessage.isEmpty() ? null : nextMessage);
        advisory.setActive(Boolean.TRUE.equals(body.getActive()));

        return ResponseEntity.ok(repository.save(advisory));
    }
}