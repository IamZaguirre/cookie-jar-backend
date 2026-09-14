package com.cookiejar.repository;

import com.cookiejar.model.PromoRedemption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PromoRedemptionRepository extends JpaRepository<PromoRedemption, Long> {
    boolean existsByPromoId(Long promoId);
    Optional<PromoRedemption> findByPromoIdAndEmail(Long promoId, String email);
    Optional<PromoRedemption> findByPromoIdAndPhone(Long promoId, String phone);
}
