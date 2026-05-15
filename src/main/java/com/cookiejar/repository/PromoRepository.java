package com.cookiejar.repository;

import com.cookiejar.model.Promo;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PromoRepository extends JpaRepository<Promo, Long> {
    List<Promo> findByActiveTrueOrderByCreatedAtDesc();
}
