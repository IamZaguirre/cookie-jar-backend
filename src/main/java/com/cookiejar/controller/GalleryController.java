package com.cookiejar.controller;

import com.cookiejar.model.GalleryPhoto;
import com.cookiejar.repository.GalleryPhotoRepository;
import com.cookiejar.service.CloudinaryService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/gallery")
public class GalleryController {

    private final GalleryPhotoRepository repository;
    private final CloudinaryService cloudinaryService;

    public GalleryController(GalleryPhotoRepository repository, CloudinaryService cloudinaryService) {
        this.repository = repository;
        this.cloudinaryService = cloudinaryService;
    }

    /** Public endpoint — only active photos ordered by sortOrder */
    @GetMapping
    public ResponseEntity<List<GalleryPhoto>> getActive() {
        return ResponseEntity.ok(repository.findByActiveTrueOrderBySortOrderAscCreatedAtDesc());
    }

    /** Admin endpoint — all photos */
    @GetMapping("/all")
    public ResponseEntity<List<GalleryPhoto>> getAll() {
        return ResponseEntity.ok(repository.findAll());
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> create(
            @RequestParam(value = "caption", required = false) String caption,
            @RequestParam(value = "sortOrder", defaultValue = "0") Integer sortOrder,
            HttpServletRequest request
    ) {
        if (!(request instanceof MultipartHttpServletRequest mReq)) {
            return ResponseEntity.badRequest().body("Multipart request required");
        }
        MultipartFile image = mReq.getFile("image");
        if (image == null || image.isEmpty()) {
            return ResponseEntity.badRequest().body("image file is required");
        }
        try {
            String url = cloudinaryService.uploadImage(image, "cookie-jar/gallery");
            GalleryPhoto photo = new GalleryPhoto();
            photo.setImageUrl(url);
            photo.setCaption(caption);
            photo.setSortOrder(sortOrder);
            return ResponseEntity.status(201).body(repository.save(photo));
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Upload failed: " + e.getMessage());
        }
    }

    @PatchMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return repository.findById(id).map(photo -> {
            if (body.containsKey("caption")) photo.setCaption((String) body.get("caption"));
            if (body.containsKey("sortOrder")) photo.setSortOrder((Integer) body.get("sortOrder"));
            if (body.containsKey("active")) photo.setActive((Boolean) body.get("active"));
            return ResponseEntity.ok(repository.save(photo));
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        if (!repository.existsById(id)) return ResponseEntity.notFound().build();
        GalleryPhoto photo = repository.findById(id).get();
        cloudinaryService.deleteImage(photo.getImageUrl());
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
