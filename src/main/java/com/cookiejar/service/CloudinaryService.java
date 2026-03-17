package com.cookiejar.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
public class CloudinaryService {

    private final Cloudinary cloudinary;

    public CloudinaryService(
            @Value("${cloudinary.cloud-name}") String cloudName,
            @Value("${cloudinary.api-key}") String apiKey,
            @Value("${cloudinary.api-secret}") String apiSecret) {
        this.cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key", apiKey,
                "api_secret", apiSecret,
                "secure", true
        ));
    }

    public String uploadImage(MultipartFile file) throws IOException {
        Map<?, ?> result = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                "folder", "cookie-jar/products",
                "resource_type", "image"
        ));
        return (String) result.get("secure_url");
    }

    public void deleteImage(String imageUrl) {
        if (imageUrl == null || !imageUrl.contains("cloudinary.com")) {
            return;
        }
        String publicId = extractPublicId(imageUrl);
        if (publicId == null) {
            return;
        }
        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
        } catch (IOException ignored) {
        }
    }

    private String extractPublicId(String url) {
        int uploadIndex = url.indexOf("/upload/");
        if (uploadIndex < 0) {
            return null;
        }
        String afterUpload = url.substring(uploadIndex + 8);

        // Strip version segment (e.g. "v1234567890/")
        if (afterUpload.startsWith("v")) {
            int slash = afterUpload.indexOf('/');
            if (slash > 0) {
                String versionPart = afterUpload.substring(1, slash);
                if (versionPart.chars().allMatch(Character::isDigit)) {
                    afterUpload = afterUpload.substring(slash + 1);
                }
            }
        }

        // Strip file extension
        int lastDot = afterUpload.lastIndexOf('.');
        if (lastDot > 0) {
            afterUpload = afterUpload.substring(0, lastDot);
        }

        return afterUpload;
    }
}
