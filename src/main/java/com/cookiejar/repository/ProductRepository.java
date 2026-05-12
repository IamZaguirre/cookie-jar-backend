package com.cookiejar.repository;

import com.cookiejar.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    @Query(value = """
        SELECT p.* FROM products p
        LEFT JOIN order_items oi ON p.id = oi.product_id
        WHERE p.active = true
        GROUP BY p.id
        ORDER BY COUNT(oi.id) DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<Product> findMostOrdered(@Param("limit") int limit);
}
