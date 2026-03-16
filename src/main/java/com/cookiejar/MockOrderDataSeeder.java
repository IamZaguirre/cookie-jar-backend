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

        List<Order> existing = orderRepository.findAll();
        Instant[] neededAtValues = {
                Instant.now().plus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.HOURS),
                Instant.now().plus(6, ChronoUnit.HOURS).truncatedTo(ChronoUnit.HOURS),
                Instant.now().minus(1, ChronoUnit.DAYS).plus(9, ChronoUnit.HOURS).truncatedTo(ChronoUnit.HOURS),
        };
        if (!existing.isEmpty()) {
            // Patch any existing mock orders that are missing customer info
            String[][] customers = {
                {"Sarah", "Mitchell", "sarah.mitchell@email.com", "+1 (555) 204-3817"},
                {"James", "Navarro", "james.navarro@email.com", "+1 (555) 371-9024"},
                {"Priya", "Okonkwo", "priya.okonkwo@email.com", "+1 (555) 489-6153"},
            };
            boolean patched = false;
            for (int i = 0; i < existing.size() && i < customers.length; i++) {
                Order o = existing.get(i);
                if (o.getFirstName() == null) {
                    o.setFirstName(customers[i][0]);
                    o.setLastName(customers[i][1]);
                    o.setEmail(customers[i][2]);
                    o.setPhone(customers[i][3]);
                    patched = true;
                }
                if (o.getNeededAt() == null) {
                    o.setNeededAt(neededAtValues[i]);
                    patched = true;
                }
                if (patched) {
                    orderRepository.save(o);
                }
            }
            if (!patched) return;
            return;
        }

        List<Product> products = ensureProducts();
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

    private List<Product> ensureProducts() {
        List<Product> products = new ArrayList<>(productRepository.findAll());
        if (products.size() >= 3) {
            topUpInventory(products.get(0), 3);
            topUpInventory(products.get(1), 4);
            topUpInventory(products.get(2), 6);
            return productRepository.saveAll(products);
        }

        while (products.size() < 3) {
            Product product = new Product();
            int index = products.size() + 1;
            product.setName("Sample Cookie Box " + index);
            product.setDescription("Mock seeded product for dashboard testing.");
            product.setPriceCents(18000 + (index * 2500));
            product.setSku("SAMPLE-COOKIE-" + index);
            product.setInventory(20);
            products.add(productRepository.save(product));
        }

        return products;
    }

    private void topUpInventory(Product product, int minimumNeeded) {
        int currentInventory = product.getInventory() == null ? 0 : product.getInventory();
        if (currentInventory < minimumNeeded + 5) {
            product.setInventory(minimumNeeded + 10);
        }
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
