package com.cookiejar.repository;

import com.cookiejar.model.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    @Modifying
    @Transactional
    @Query("UPDATE OrderItem oi SET oi.variant = NULL WHERE oi.variant.id IN :variantIds")
    void nullifyVariantReferences(@Param("variantIds") List<Long> variantIds);
}
