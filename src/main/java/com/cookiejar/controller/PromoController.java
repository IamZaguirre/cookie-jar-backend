package com.cookiejar.controller;

import com.cookiejar.model.Promo;
import com.cookiejar.repository.PromoRepository;
import com.cookiejar.service.CloudinaryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/promos")
public class PromoController {

    private final PromoRepository repository;
    private final CloudinaryService cloudinaryService;

    public PromoController(PromoRepository repository, CloudinaryService cloudinaryService) {
        this.repository = repository;
        this.cloudinaryService = cloudinaryService;
    }

    /** Public endpoint — only active promos */
    @GetMapping
    public ResponseEntity<List<Promo>> getActive() {
        return ResponseEntity.ok(repository.findByActiveTrueOrderByCreatedAtDesc());
    }

    /** Admin endpoint — all promos regardless of active flag */
    @GetMapping("/all")
    public ResponseEntity<List<Promo>> getAll() {
        return ResponseEntity.ok(repository.findAll());
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> create(
            @RequestPart("promo") String promoJson,
            HttpServletRequest request
    ) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
            Promo p = mapper.readValue(promoJson, Promo.class);

            if (request instanceof MultipartHttpServletRequest mReq) {
                MultipartFile image = mReq.getFile("image");
                if (image != null && !image.isEmpty()) {
                    String url = cloudinaryService.uploadImage(image, "cookie-jar/promos");
                    p.setImageUrl(url);
                }
            }

            return ResponseEntity.status(201).body(repository.save(p));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Invalid promo data: " + e.getMessage());
        }
    }

    @PatchMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable("id") Long id, @RequestBody Promo body) {
        return repository.findById(id).map(promo -> {
            if (body.getTitle() != null) promo.setTitle(body.getTitle());
            if (body.getDescription() != null) promo.setDescription(body.getDescription());
            if (body.getDiscountCode() != null) promo.setDiscountCode(body.getDiscountCode());
            if (body.getValidUntil() != null) promo.setValidUntil(body.getValidUntil());
            if (body.getActive() != null) promo.setActive(body.getActive());
            return ResponseEntity.ok(repository.save(promo));
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        if (!repository.existsById(id)) return ResponseEntity.notFound().build();
        Promo promo = repository.findById(id).get();
        if (promo.getImageUrl() != null) {
            try {
                cloudinaryService.deleteImage(promo.getImageUrl());
            } catch (Exception e) {
                // Log but don't block deletion if image removal fails
                System.err.println("Failed to delete promo image from Cloudinary: " + e.getMessage());
            }
        }
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
