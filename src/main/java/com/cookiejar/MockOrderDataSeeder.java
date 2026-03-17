package com.cookiejar;

import com.cookiejar.model.Admin;
import com.cookiejar.model.Order;
import com.cookiejar.model.OrderItem;
import com.cookiejar.model.Product;
import com.cookiejar.repository.AdminRepository;
import com.cookiejar.repository.OrderRepository;
import com.cookiejar.repository.ProductRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
public class MockOrderDataSeeder implements CommandLineRunner {
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final AdminRepository adminRepository;

    public MockOrderDataSeeder(
            OrderRepository orderRepository,
            ProductRepository productRepository,
            AdminRepository adminRepository
    ) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.adminRepository = adminRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        ensureAdminUser("admin@cookie.com", "abc123", "Cookie Jar Admin");
        Admin admin = ensureAdminUser("ana@cookie.com", "AnaWanda08!", "Ana");

        // Skip seeding entirely if the database already has any data.
        // This ensures re-deployments never overwrite products or orders
        // that were created or edited in production.
        if (productRepository.count() > 0 || orderRepository.count() > 0) {
            return;
        }

        List<Product> products = seedProducts();
        if (products.size() < 3) {
            return;
        }

        List<Order> mockOrders = new ArrayList<>();
        mockOrders.add(buildOrder(
                admin,
                "pending",
                Instant.now().minus(3, ChronoUnit.HOURS),
            Instant.now().plus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.HOURS),
                "Sarah", "Mitchell", "sarah.mitchell@email.com", "+1 (555) 204-3817",
                Arrays.asList(
                        new OrderLine(products.get(0), 2),
                        new OrderLine(products.get(1), 1)
                )
        ));
        mockOrders.add(buildOrder(
                admin,
                "ready",
                Instant.now().minus(26, ChronoUnit.HOURS),
            Instant.now().plus(6, ChronoUnit.HOURS).truncatedTo(ChronoUnit.HOURS),
                "James", "Navarro", "james.navarro@email.com", "+1 (555) 371-9024",
                Arrays.asList(
                        new OrderLine(products.get(1), 3),
                        new OrderLine(products.get(2), 2)
                )
        ));
        mockOrders.add(buildOrder(
                admin,
                "fulfilled",
                Instant.now().minus(3, ChronoUnit.DAYS),
            Instant.now().minus(1, ChronoUnit.DAYS).plus(9, ChronoUnit.HOURS).truncatedTo(ChronoUnit.HOURS),
                "Priya", "Okonkwo", "priya.okonkwo@email.com", "+1 (555) 489-6153",
                Arrays.asList(
                        new OrderLine(products.get(0), 1),
                        new OrderLine(products.get(2), 4)
                )
        ));

        orderRepository.saveAll(mockOrders);
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

    private List<Product> seedProducts() {
        List<Product> products = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            Product product = new Product();
            product.setName("Sample Cookie Box " + i);
            product.setDescription("Mock seeded product for dashboard testing.");
            product.setPriceCents(18000 + (i * 2500));
            product.setSku("SAMPLE-COOKIE-" + i);
            product.setInventory(20);
            products.add(productRepository.save(product));
        }
        return products;
    }

    private Order buildOrder(
            Admin admin,
            String status,
            Instant createdAt,
            Instant neededAt,
            String firstName,
            String lastName,
            String email,
            String phone,
            List<OrderLine> lines
    ) {
        Order order = new Order();
        order.setCreatedBy(admin);
        order.setStatus(status);
        order.setCreatedAt(createdAt);
        order.setNeededAt(neededAt);
        order.setFirstName(firstName);
        order.setLastName(lastName);
        order.setEmail(email);
        order.setPhone(phone);

        List<OrderItem> items = new ArrayList<>();
        int totalCents = 0;

        for (OrderLine line : lines) {
            Product product = line.getProduct();
            product.setInventory(product.getInventory() - line.getQuantity());
            productRepository.save(product);

            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setProduct(product);
            item.setQuantity(line.getQuantity());
            item.setUnitPrice(product.getPriceCents());
            items.add(item);
            totalCents += product.getPriceCents() * line.getQuantity();
        }

        order.setItems(items);
        order.setTotalCents(totalCents);
        return order;
    }

    private static class OrderLine {
        private final Product product;
        private final int quantity;

        private OrderLine(Product product, int quantity) {
            this.product = product;
            this.quantity = quantity;
        }

        private Product getProduct() {
            return product;
        }

        private int getQuantity() {
            return quantity;
        }
    }
}
