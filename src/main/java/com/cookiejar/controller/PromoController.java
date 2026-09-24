package com.cookiejar.controller;

import com.cookiejar.dto.ValidatePromoRequest;
import com.cookiejar.model.Promo;
import com.cookiejar.model.PromoRedemption;
import com.cookiejar.repository.PromoRedemptionRepository;
import com.cookiejar.repository.PromoRepository;
import com.cookiejar.service.CloudinaryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/promos")
public class PromoController {

    private final PromoRepository repository;
    private final PromoRedemptionRepository redemptionRepository;
    private final CloudinaryService cloudinaryService;

    public PromoController(PromoRepository repository, PromoRedemptionRepository redemptionRepository, CloudinaryService cloudinaryService) {
        this.repository = repository;
        this.redemptionRepository = redemptionRepository;
        this.cloudinaryService = cloudinaryService;
    }

    /** Public endpoint — only active promos */
    @GetMapping
    public ResponseEntity<List<Promo>> getActive() {
        return ResponseEntity.ok(repository.findByActiveTrueOrderByCreatedAtDesc());
    }

    @PostMapping("/validate")
    public ResponseEntity<?> validatePromo(@Valid @RequestBody ValidatePromoRequest body) {
        if (body.getPromoCode() == null || body.getPromoCode().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Promo code is required"));
        }
        if (body.getEmail() == null || body.getEmail().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Email is required"));
        }
        if (body.getPhone() == null || body.getPhone().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Phone is required"));
        }

        String normalizedCode = body.getPromoCode().trim();
        Optional<Promo> promoOpt = repository.findAll().stream()
                .filter(promo -> promo.getDiscountCode() != null && promo.getDiscountCode().equalsIgnoreCase(normalizedCode))
                .filter(Promo::getActive)
                .findFirst();

        if (promoOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "This promo code is not valid or has expired."));
        }

        Promo promo = promoOpt.get();
        if (promo.getValidUntil() != null && promo.getValidUntil().isBefore(LocalDate.now())) {
            return ResponseEntity.badRequest().body(Map.of("message", "This promo code has expired."));
        }

        String normalizedEmail = body.getEmail().trim();
        String normalizedPhone = body.getPhone().trim();

        if ("VOUCHER".equalsIgnoreCase(promo.getPromoType())
                && redemptionRepository.existsByPromoId(promo.getId())) {
            return ResponseEntity.badRequest().body(Map.of("message", "This voucher code has already been used."));
        }

        Optional<PromoRedemption> byEmail = redemptionRepository.findByPromoIdAndEmail(promo.getId(), normalizedEmail);
        if (byEmail.isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("message", "This promo code has already been used for this email address."));
        }

        Optional<PromoRedemption> byPhone = redemptionRepository.findByPromoIdAndPhone(promo.getId(), normalizedPhone);
        if (byPhone.isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("message", "This promo code has already been used for this phone number."));
        }

        return ResponseEntity.ok(Map.of(
                "valid", true,
                "promoId", promo.getId(),
                "discountCode", promo.getDiscountCode()
        ));
    }

    @PostMapping("/redeem")
    public ResponseEntity<?> redeemPromo(@RequestBody ValidatePromoRequest body) {
        ResponseEntity<?> validation = validatePromo(body);
        if (validation.getStatusCode().isError()) {
            return validation;
        }

        Promo promo = repository.findAll().stream()
                .filter(item -> item.getDiscountCode() != null && item.getDiscountCode().equalsIgnoreCase(body.getPromoCode().trim()))
                .filter(Promo::getActive)
                .findFirst()
                .orElse(null);

        if (promo == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Promo not found."));
        }

        String email = body.getEmail().trim();
        String phone = body.getPhone().trim();

        PromoRedemption redemption = new PromoRedemption();
        redemption.setPromoId(promo.getId());
        redemption.setEmail(email);
        redemption.setPhone(phone);
        if ("VOUCHER".equalsIgnoreCase(promo.getPromoType())) {
            redemption.setRedemptionKey("VOUCHER:" + promo.getId());
        }
        redemptionRepository.save(redemption);

        return ResponseEntity.ok(Map.of(
                "redeemed", true,
                "promoId", promo.getId(),
                "discountCode", promo.getDiscountCode()
        ));
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
