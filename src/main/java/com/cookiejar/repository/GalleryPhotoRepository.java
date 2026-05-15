package com.cookiejar.repository;

import com.cookiejar.model.GalleryPhoto;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface GalleryPhotoRepository extends JpaRepository<GalleryPhoto, Long> {
    List<GalleryPhoto> findByActiveTrueOrderBySortOrderAscCreatedAtDesc();
}
