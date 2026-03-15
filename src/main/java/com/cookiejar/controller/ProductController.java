package com.cookiejar.controller;

import com.cookiejar.model.Product;
import com.cookiejar.repository.ProductRepository;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/products")
public class ProductController {
    private static final Path IMAGE_UPLOAD_DIR = Paths.get("uploads", "products");

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
    public ResponseEntity<Product> get(@PathVariable("id") Long id) {
        return repository.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable("id") Long id, @RequestBody Product p) {
        return repository.findById(id).map(e -> {
            if (p.getName()!=null) e.setName(p.getName());
            if (p.getDescription()!=null) e.setDescription(p.getDescription());
            if (p.getPriceCents()!=null) e.setPriceCents(p.getPriceCents());
            if (p.getSku()!=null) e.setSku(p.getSku());
            if (p.getImageUrl()!=null) e.setImageUrl(p.getImageUrl());
            if (p.getInventory()!=null) e.setInventory(p.getInventory());
            repository.save(e);
            return ResponseEntity.ok(e);
        }).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping(value = "/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadImage(@PathVariable("id") Long id, @RequestParam("image") MultipartFile image) {
        if (image.isEmpty()) {
            return ResponseEntity.badRequest().body("image file is required");
        }

        return repository.findById(id).map(product -> {
            try {
                Files.createDirectories(IMAGE_UPLOAD_DIR);

                String extension = getFileExtension(image.getOriginalFilename());
                String fileName = UUID.randomUUID() + extension;
                Path target = IMAGE_UPLOAD_DIR.resolve(fileName).normalize();

                Files.copy(image.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

                deleteExistingImage(product.getImageUrl());

                product.setImageUrl("/api/products/images/" + fileName);
                repository.save(product);
                return ResponseEntity.ok(product);
            } catch (IOException ex) {
                return ResponseEntity.internalServerError().body("failed to store image");
            } catch (RuntimeException ex) {
                return ResponseEntity.badRequest().body("invalid image filename");
            }
        }).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/images/{fileName:.+}")
    public ResponseEntity<?> getImage(@PathVariable("fileName") String fileName) {
        try {
            Path uploadRoot = IMAGE_UPLOAD_DIR.toAbsolutePath().normalize();
            Path file = uploadRoot.resolve(fileName).normalize();

            if (!file.startsWith(uploadRoot)) {
                return ResponseEntity.badRequest().body("invalid image path");
            }

            if (!Files.exists(file) || !Files.isRegularFile(file)) {
                return ResponseEntity.notFound().build();
            }

            Resource resource = new UrlResource(file.toUri());
            String contentType = Files.probeContentType(file);
            MediaType mediaType;
            try {
                mediaType = contentType != null
                        ? MediaType.parseMediaType(contentType)
                        : MediaType.APPLICATION_OCTET_STREAM;
            } catch (IllegalArgumentException ex) {
                mediaType = MediaType.APPLICATION_OCTET_STREAM;
            }

            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);
        } catch (MalformedURLException ex) {
            return ResponseEntity.internalServerError().body("failed to read image");
        } catch (IOException ex) {
            return ResponseEntity.internalServerError().body("failed to read image metadata");
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        return repository.findById(id).map(product -> {
            deleteExistingImage(product.getImageUrl());
            repository.deleteById(id);
            return ResponseEntity.noContent().<Void>build();
        }).orElse(ResponseEntity.notFound().build());
    }

    private void deleteExistingImage(String imageUrl) {
        if (!StringUtils.hasText(imageUrl)) {
            return;
        }

        String prefix = "/api/products/images/";
        if (!imageUrl.startsWith(prefix)) {
            return;
        }

        String fileName = imageUrl.substring(prefix.length());
        Path file = IMAGE_UPLOAD_DIR.resolve(fileName).normalize();

        try {
            Files.deleteIfExists(file);
        } catch (IOException ignored) {
        }
    }

    private String getFileExtension(String originalFileName) {
        if (!StringUtils.hasText(originalFileName)) {
            return "";
        }

        String sanitized = originalFileName.replace("\\", "/");
        int slash = sanitized.lastIndexOf('/');
        String baseName = slash >= 0 ? sanitized.substring(slash + 1) : sanitized;

        int lastDot = baseName.lastIndexOf('.');
        if (lastDot < 0) {
            return "";
        }

        return baseName.substring(lastDot).toLowerCase(Locale.ROOT);
    }
}
