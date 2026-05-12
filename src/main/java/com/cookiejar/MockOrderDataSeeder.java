package com.cookiejar;

import com.cookiejar.model.Admin;
import com.cookiejar.model.Product;
import com.cookiejar.repository.AdminRepository;
import com.cookiejar.repository.ProductRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Component
public class MockOrderDataSeeder implements CommandLineRunner {
    private final ProductRepository productRepository;
    private final AdminRepository adminRepository;

    public MockOrderDataSeeder(
            ProductRepository productRepository,
            AdminRepository adminRepository
    ) {
        this.productRepository = productRepository;
        this.adminRepository = adminRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        ensureAdminUser("admin@cookie.com", "abc123", "Cookie Jar Admin");
        ensureAdminUser("ana@cookie.com", "AnaWanda08!", "Ana");

    }

    private Admin ensureAdminUser(String email, String password, String name) {
        return adminRepository.findByEmail(email)
                .orElseGet(() -> {
                    Admin newAdmin = new Admin();
                    newAdmin.setEmail(email);
                    newAdmin.setPassword(password);
                    newAdmin.setName(name);
                    return adminRepository.save(newAdmin);
                });
    }

    private void seedProducts() {
        for (int i = 1; i <= 3; i++) {
            Product product = new Product();
            product.setName("Sample Cookie Box " + i);
            product.setDescription("Mock seeded product for dashboard testing.");
            product.setPriceCents(18000 + (i * 2500));
            product.setSku("SAMPLE-COOKIE-" + i);
            product.setInventory(20);
            productRepository.save(product);
        }
    }
}
