package com.cookiejar.repository;

import com.cookiejar.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface OrderRepository extends JpaRepository<Order, String> {

    @Query("SELECT COALESCE(SUM(oi.quantity), 0) FROM OrderItem oi " +
           "WHERE oi.product.id = :productId " +
           "AND oi.order.neededAt >= :dayStart " +
           "AND oi.order.neededAt < :dayEnd " +
           "AND oi.order.status <> 'cancelled'")
    int sumQtyForProductOnDay(@Param("productId") Long productId,
                              @Param("dayStart") Instant dayStart,
                              @Param("dayEnd") Instant dayEnd);

    @Query("SELECT COALESCE(SUM(oi.quantity), 0) FROM OrderItem oi " +
           "WHERE oi.variant.id = :variantId " +
           "AND oi.order.neededAt >= :dayStart " +
           "AND oi.order.neededAt < :dayEnd " +
           "AND oi.order.status <> 'cancelled'")
    int sumQtyForVariantOnDay(@Param("variantId") Long variantId,
                              @Param("dayStart") Instant dayStart,
                              @Param("dayEnd") Instant dayEnd);
}
