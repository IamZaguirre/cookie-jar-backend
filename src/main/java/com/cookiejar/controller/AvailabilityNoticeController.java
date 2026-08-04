package com.cookiejar.controller;

import com.cookiejar.model.AvailabilityNotice;
import com.cookiejar.repository.AvailabilityNoticeRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/availability-notice")
public class AvailabilityNoticeController {

    private final AvailabilityNoticeRepository repository;

    public AvailabilityNoticeController(AvailabilityNoticeRepository repository) {
        this.repository = repository;
    }

    /** Public endpoint — returns the message for the order flow banner. */
    @GetMapping
    public ResponseEntity<?> get() {
        String msg = repository.findById(AvailabilityNotice.SINGLETON_ID)
                .map(AvailabilityNotice::getMessage)
                .orElse(null);
        if (msg == null || msg.isBlank()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(Map.of("message", msg));
    }

    /** Admin endpoint — save or clear the notice message. */
    @PutMapping("/admin")
    public ResponseEntity<AvailabilityNotice> upsert(
            @RequestParam(name = "message", required = false) String message
    ) {
        AvailabilityNotice notice = repository
                .findById(AvailabilityNotice.SINGLETON_ID)
                .orElseGet(AvailabilityNotice::new);
        notice.setId(AvailabilityNotice.SINGLETON_ID);
        notice.setMessage(message != null ? message.strip() : null);
        return ResponseEntity.ok(repository.save(notice));
    }
}
