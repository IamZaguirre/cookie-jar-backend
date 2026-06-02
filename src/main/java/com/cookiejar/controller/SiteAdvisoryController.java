package com.cookiejar.controller;

import com.cookiejar.model.SiteAdvisory;
import com.cookiejar.repository.SiteAdvisoryRepository;
import com.cookiejar.service.CloudinaryService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

@RestController
@RequestMapping("/api/site-advisory")
public class SiteAdvisoryController {

    private final SiteAdvisoryRepository repository;
    private final CloudinaryService cloudinaryService;

    public SiteAdvisoryController(SiteAdvisoryRepository repository, CloudinaryService cloudinaryService) {
        this.repository = repository;
        this.cloudinaryService = cloudinaryService;
    }

    @GetMapping
    public ResponseEntity<SiteAdvisory> getActive() {
        return repository
                .findById(SiteAdvisory.SINGLETON_ID)
                .filter(advisory -> Boolean.TRUE.equals(advisory.getActive()))
                .filter(advisory -> advisory.getImageUrl() != null)
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

    @PutMapping(value = "/admin", consumes = { MediaType.MULTIPART_FORM_DATA_VALUE, MediaType.APPLICATION_FORM_URLENCODED_VALUE })
    public ResponseEntity<?> upsert(
            @RequestParam(name = "active", required = false) Boolean active,
            HttpServletRequest request
    ) {
        try {
            boolean isActive = Boolean.TRUE.equals(active);

            SiteAdvisory advisory = repository
                    .findById(SiteAdvisory.SINGLETON_ID)
                    .orElseGet(SiteAdvisory::new);

            advisory.setId(SiteAdvisory.SINGLETON_ID);
            advisory.setActive(isActive);

            if (request instanceof MultipartHttpServletRequest mReq) {
                MultipartFile image = mReq.getFile("image");
                if (image != null && !image.isEmpty()) {
                    String existingImageUrl = advisory.getImageUrl();
                    if (existingImageUrl != null) {
                        cloudinaryService.deleteImage(existingImageUrl);
                    }
                    String url = cloudinaryService.uploadImage(image, "cookie-jar/advisory");
                    advisory.setImageUrl(url);
                }
            }

            if (isActive && advisory.getImageUrl() == null) {
                return ResponseEntity.badRequest().body("Active advisory requires an image.");
            }

            return ResponseEntity.ok(repository.save(advisory));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to update advisory: " + e.getMessage());
        }
    }
}