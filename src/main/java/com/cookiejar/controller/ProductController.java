package com.cookiejar.controller;

import com.cookiejar.model.Product;
import com.cookiejar.model.Variant;
import com.cookiejar.repository.ProductRepository;
import com.cookiejar.repository.VariantRepository;
import com.cookiejar.service.CloudinaryService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.bind.annotation.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
public class ProductController {
    private final ProductRepository repository;
    private final VariantRepository variantRepository;
    private final CloudinaryService cloudinaryService;

    private static final Logger logger = LoggerFactory.getLogger(ProductController.class);

    public ProductController(ProductRepository repository, VariantRepository variantRepository, CloudinaryService cloudinaryService) {
        this.repository = repository;
        this.variantRepository = variantRepository;
        this.cloudinaryService = cloudinaryService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> create(
            @RequestPart("product") String productJson,
            HttpServletRequest request
    ) {
        List<MultipartFile> images = new ArrayList<>();
        if (request instanceof MultipartHttpServletRequest) {
            images = ((MultipartHttpServletRequest) request).getFiles("images");
        }
        logger.info("[CREATE PRODUCT] images count={}", images.size());
        try {
            logger.info("[CREATE PRODUCT] Received productJson: {}", productJson);
            ObjectMapper mapper = new ObjectMapper();
            Product p = mapper.readValue(productJson, Product.class);
            // Handle variants if present
            com.fasterxml.jackson.databind.JsonNode rootNode = mapper.readTree(productJson);
            com.fasterxml.jackson.databind.JsonNode variantsNode = rootNode.get("variants");
            if (variantsNode != null && variantsNode.isArray()) {
                p.getVariants().clear();
                for (com.fasterxml.jackson.databind.JsonNode vNode : variantsNode) {
                    String vName = vNode.get("name").asText();
                    int vInventory = vNode.get("inventory").asInt();
                    int vPriceCents = vNode.has("priceCents")
                        ? vNode.get("priceCents").asInt()
                        : (vNode.has("price") ? (int) Math.round(vNode.get("price").asDouble() * 100) : 0);
                    Variant variant = new Variant();
                    variant.setName(vName);
                    variant.setInventory(vInventory);
                    variant.setPriceCents(vPriceCents);
                    if (vNode.has("discountPercent") && !vNode.get("discountPercent").isNull()) {
                        variant.setDiscountPercent(vNode.get("discountPercent").asDouble());
                    }
                    variant.setProduct(p);
                    p.getVariants().add(variant);
                }
            }
            // Debug: log the type and value of inventory
            com.fasterxml.jackson.databind.JsonNode rootNode1 = mapper.readTree(productJson);
            com.fasterxml.jackson.databind.JsonNode inventoryNode = rootNode1.get("inventory");
            logger.info(
                "[CREATE PRODUCT] Parsed Product: name={}, priceCents={}, inventory={}, rawInventoryType={}, rawInventoryValue={}",
                p.getName(),
                p.getPriceCents(),
                p.getInventory(),
                inventoryNode != null ? inventoryNode.getNodeType() : "null",
                inventoryNode != null ? inventoryNode.toString() : "null"
            );
            if (p.getName()==null || p.getPriceCents()==null) {
                logger.warn("[CREATE PRODUCT] name and priceCents required");
                return ResponseEntity.badRequest().body(Map.of("message", "name and priceCents required"));
            }
            if (p.getInventory() == null) p.setInventory(0);
            // Set category if present in JSON
            com.fasterxml.jackson.databind.JsonNode categoryNode = rootNode1.get("category");
            if (categoryNode != null && !categoryNode.isNull()) {
                p.setCategory(categoryNode.asText());
            }
            // Set minHours if present in JSON
            com.fasterxml.jackson.databind.JsonNode minHoursNode = rootNode1.get("minHours");
            if (minHoursNode != null && !minHoursNode.isNull() && minHoursNode.isInt()) {
                p.setMinHours(minHoursNode.asInt());
            }
            // Set discountPercent if present in JSON
            com.fasterxml.jackson.databind.JsonNode discountNode = rootNode1.get("discountPercent");
            if (discountNode != null && !discountNode.isNull() && discountNode.isNumber()) {
                p.setDiscountPercent(discountNode.asDouble());
            }
            // Set active if present in JSON (defaults to true if absent)
            com.fasterxml.jackson.databind.JsonNode activeNode = rootNode1.get("active");
            if (activeNode != null && !activeNode.isNull()) {
                p.setActive(activeNode.asBoolean());
            } else {
                p.setActive(true);
            }
            // Upload all images; first becomes imageUrl, rest go to imageUrls
            if (images != null && !images.isEmpty()) {
                List<String> uploadedUrls = new ArrayList<>();
                for (MultipartFile img : images) {
                    if (img != null && !img.isEmpty()) {
                        uploadedUrls.add(cloudinaryService.uploadImage(img));
                    }
                }
                if (!uploadedUrls.isEmpty()) {
                    p.setImageUrl(uploadedUrls.get(0));
                    p.setImageUrls(new ArrayList<>(uploadedUrls.subList(1, uploadedUrls.size())));
                }
            }
            Product saved = repository.save(p);
            if (p.getVariants() != null && !p.getVariants().isEmpty()) {
                for (Variant v : p.getVariants()) {
                    v.setProduct(saved);
                }
                variantRepository.saveAll(p.getVariants());
            }
            logger.info("[CREATE PRODUCT] Product saved with id={}", saved.getId());
            return ResponseEntity.status(201).body(saved);
        } catch (Exception e) {
            logger.error("[CREATE PRODUCT] Product creation failed", e);
            return ResponseEntity.internalServerError().body(Map.of("message", "Product creation failed: " + e.getMessage()));
        }
    }

    @GetMapping
    public List<Product> list() { return repository.findAll(); }

    @GetMapping("/{id}")
    public ResponseEntity<Product> get(@PathVariable("id") Long id) {
        return repository.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> update(
            @PathVariable("id") Long id,
            @RequestPart("product") String productJson,
            HttpServletRequest request
    ) {
        List<MultipartFile> imagesTmp = new ArrayList<>();
        if (request instanceof MultipartHttpServletRequest) {
            imagesTmp = ((MultipartHttpServletRequest) request).getFiles("images");
        }
        final List<MultipartFile> images = imagesTmp;
        logger.info("[UPDATE PRODUCT] images count={}", images.size());
        try {
            logger.info("[UPDATE PRODUCT] Received productJson: {} for id={}", productJson, id);
            ObjectMapper mapper = new ObjectMapper();
            Product p = mapper.readValue(productJson, Product.class);
            com.fasterxml.jackson.databind.JsonNode rootNode = mapper.readTree(productJson);
            com.fasterxml.jackson.databind.JsonNode variantsNode = rootNode.get("variants");
            // Parse keepImageUrls - existing URLs the client wants to retain
            List<String> keepImageUrls = new ArrayList<>();
            com.fasterxml.jackson.databind.JsonNode keepNode = rootNode.get("keepImageUrls");
            if (keepNode != null && keepNode.isArray()) {
                for (com.fasterxml.jackson.databind.JsonNode urlNode : keepNode) {
                    keepImageUrls.add(urlNode.asText());
                }
            }
            logger.info("[UPDATE PRODUCT] Parsed Product: name={}, priceCents={}, inventory={}, keepImageUrls={}", p.getName(), p.getPriceCents(), p.getInventory(), keepImageUrls);
            final List<String> finalKeepImageUrls = keepImageUrls;
            return repository.findById(id)
                .<ResponseEntity<?>>map(e -> {
                    try {
                        if (p.getName()!=null) e.setName(p.getName());
                        if (p.getDescription()!=null) e.setDescription(p.getDescription());
                        if (p.getPriceCents()!=null) e.setPriceCents(p.getPriceCents());
                        if (p.getSku()!=null) e.setSku(p.getSku());
                        if (p.getInventory()!=null) e.setInventory(p.getInventory());
                        if (p.getCategory()!=null) e.setCategory(p.getCategory());
                        // Fix minHours update logic: prefer JSON node if present, fallback to POJO
                        com.fasterxml.jackson.databind.JsonNode minHoursNode = rootNode.get("minHours");
                        if (minHoursNode != null && !minHoursNode.isNull() && minHoursNode.isInt()) {
                            e.setMinHours(minHoursNode.asInt());
                        } else if (p.getMinHours() != null) {
                            e.setMinHours(p.getMinHours());
                        }
                        // Handle discountPercent field
                        com.fasterxml.jackson.databind.JsonNode discountNode = rootNode.get("discountPercent");
                        if (discountNode != null && !discountNode.isNull() && discountNode.isNumber()) {
                            e.setDiscountPercent(discountNode.asDouble());
                        } else if (discountNode != null && discountNode.isNull()) {
                            e.setDiscountPercent(null);
                        }
                        // Handle active field
                        com.fasterxml.jackson.databind.JsonNode activeNode = rootNode.get("active");
                        if (activeNode != null && !activeNode.isNull()) {
                            e.setActive(activeNode.asBoolean());
                        }
                        // Determine which old URLs are being removed and delete them from Cloudinary
                        List<String> allOldUrls = new ArrayList<>();
                        if (e.getImageUrl() != null) allOldUrls.add(e.getImageUrl());
                        allOldUrls.addAll(e.getImageUrls());
                        for (String oldUrl : allOldUrls) {
                            if (!finalKeepImageUrls.contains(oldUrl)) {
                                cloudinaryService.deleteImage(oldUrl);
                            }
                        }
                        // Upload new images
                        List<String> newUploadedUrls = new ArrayList<>();
                        if (images != null) {
                            for (MultipartFile img : images) {
                                if (img != null && !img.isEmpty()) {
                                    newUploadedUrls.add(cloudinaryService.uploadImage(img));
                                }
                            }
                        }
                        // Combine: kept existing first, then newly uploaded
                        List<String> allUrls = new ArrayList<>(finalKeepImageUrls);
                        allUrls.addAll(newUploadedUrls);
                        if (!allUrls.isEmpty()) {
                            e.setImageUrl(allUrls.get(0));
                            e.setImageUrls(new ArrayList<>(allUrls.subList(1, allUrls.size())));
                        } else {
                            e.setImageUrl(null);
                            e.setImageUrls(new ArrayList<>());
                        }
                        // Update variants if present
                        if (variantsNode != null && variantsNode.isArray()) {
                            e.getVariants().clear();
                            for (com.fasterxml.jackson.databind.JsonNode vNode : variantsNode) {
                                String vName = vNode.get("name").asText();
                                int vInventory = vNode.get("inventory").asInt();
                                int vPriceCents = vNode.has("priceCents")
                                    ? vNode.get("priceCents").asInt()
                                    : (vNode.has("price") ? (int) Math.round(vNode.get("price").asDouble() * 100) : 0);
                                Variant variant = new Variant();
                                variant.setName(vName);
                                variant.setInventory(vInventory);
                                variant.setPriceCents(vPriceCents);
                                if (vNode.has("discountPercent") && !vNode.get("discountPercent").isNull()) {
                                    variant.setDiscountPercent(vNode.get("discountPercent").asDouble());
                                }
                                variant.setProduct(e);
                                e.getVariants().add(variant);
                            }
                            variantRepository.saveAll(e.getVariants());
                        }
                        repository.save(e);
                        logger.info("[UPDATE PRODUCT] Product updated with id={}", e.getId());
                        return ResponseEntity.ok(e);
                    } catch (Exception ex) {
                        logger.error("[UPDATE PRODUCT] Product update failed for id=" + id, ex);
                        return ResponseEntity.internalServerError().body(Map.of("message", "Product update failed: " + ex.getMessage()));
                    }
                })
                .orElseGet(() -> {
                    logger.warn("[UPDATE PRODUCT] Product not found for id={}", id);
                    return ResponseEntity.status(404).body(Map.of("message", "Product not found"));
                });
        } catch (Exception e) {
            logger.error("[UPDATE PRODUCT] Product update failed for id=" + id, e);
            return ResponseEntity.internalServerError().body(Map.of("message", "Product update failed: " + e.getMessage()));
        }
    }

    @PostMapping(value = "/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadImage(@PathVariable("id") Long id, @RequestParam("image") MultipartFile image) {
        if (image.isEmpty()) {
            return ResponseEntity.badRequest().body("image file is required");
        }

        return repository.findById(id).map(product -> {
            try {
                String newImageUrl = cloudinaryService.uploadImage(image);
                cloudinaryService.deleteImage(product.getImageUrl());
                product.setImageUrl(newImageUrl);
                repository.save(product);
                return ResponseEntity.ok(product);
            } catch (IOException ex) {
                return ResponseEntity.internalServerError().body("failed to store image");
            }
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        return repository.findById(id).map(product -> {
            cloudinaryService.deleteImage(product.getImageUrl());
            for (String url : product.getImageUrls()) {
                cloudinaryService.deleteImage(url);
            }
            repository.deleteById(id);
            return ResponseEntity.noContent().<Void>build();
        }).orElse(ResponseEntity.notFound().build());
    }
}
