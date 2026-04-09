package com.cookiejar.service;

import com.cookiejar.model.Order;
import com.cookiejar.model.OrderItem;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.text.NumberFormat;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final String adminEmail;
    private final String fromEmail;

    public EmailService(JavaMailSender mailSender,
                        @Value("${app.admin-email}") String adminEmail,
                        @Value("${spring.mail.username}") String fromEmail) {
        this.mailSender = mailSender;
        this.adminEmail = adminEmail;
        this.fromEmail = fromEmail;
    }

    public void sendNewOrderNotification(Order order) {
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(adminEmail);
            helper.setSubject("New Order #" + order.getId() + " from " + order.getFirstName() + " " + order.getLastName());
            helper.setText(buildNewOrderHtml(order), true);
            mailSender.send(msg);
        } catch (MessagingException e) {
            System.err.println("Failed to send new order email: " + e.getMessage());
        }
    }

    public void sendStatusUpdateNotification(Order order) {
        if (order.getEmail() == null || order.getEmail().isBlank()) return;
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(order.getEmail());
            helper.setSubject("Your Order #" + order.getId() + " Status Update");
            helper.setText(buildStatusUpdateHtml(order), true);
            mailSender.send(msg);
        } catch (MessagingException e) {
            System.err.println("Failed to send status update email: " + e.getMessage());
        }
    }

    private String buildNewOrderHtml(Order order) {
        NumberFormat currency = NumberFormat.getCurrencyInstance(new Locale("en", "PH"));
        StringBuilder rows = new StringBuilder();
        for (OrderItem item : order.getItems()) {
            rows.append("<tr>")
                .append("<td style='padding:6px 12px;border-bottom:1px solid #eee;'>").append(item.getProduct().getName()).append("</td>")
                .append("<td style='padding:6px 12px;text-align:center;border-bottom:1px solid #eee;'>").append(item.getQuantity()).append("</td>")
                .append("<td style='padding:6px 12px;text-align:right;border-bottom:1px solid #eee;'>").append(currency.format(item.getUnitPrice() / 100.0)).append("</td>")
                .append("</tr>");
        }
        String neededAt = order.getNeededAt() != null
                ? DateTimeFormatter.ofPattern("MMM d, yyyy hh:mm a")
                        .withZone(ZoneId.of("Asia/Manila"))
                        .format(order.getNeededAt())
                : "Not specified";

        return "<html><body style='font-family:Arial,sans-serif;color:#333;max-width:600px;margin:auto;'>"
                + "<h2 style='color:#e91e8c;'>New Order Received!</h2>"
                + "<p><strong>Order ID:</strong> #" + order.getId() + "</p>"
                + "<hr style='border:none;border-top:1px solid #eee;'/>"
                + "<h3>Customer Details</h3>"
                + "<p><strong>Name:</strong> " + order.getFirstName() + " " + order.getLastName() + "</p>"
                + "<p><strong>Email:</strong> " + (order.getEmail() != null ? order.getEmail() : "—") + "</p>"
                + "<p><strong>Phone:</strong> " + (order.getPhone() != null ? order.getPhone() : "—") + "</p>"
                + "<p><strong>Needed By:</strong> " + neededAt + "</p>"
                + "<hr style='border:none;border-top:1px solid #eee;'/>"
                + "<h3>Items</h3>"
                + "<table style='border-collapse:collapse;width:100%;'>"
                + "<thead><tr style='background:#fce4f0;'>"
                + "<th style='padding:8px 12px;text-align:left;'>Product</th>"
                + "<th style='padding:8px 12px;text-align:center;'>Qty</th>"
                + "<th style='padding:8px 12px;text-align:right;'>Unit Price</th>"
                + "</tr></thead><tbody>" + rows + "</tbody></table>"
                + "<p style='font-size:1.1em;margin-top:12px;'><strong>Total: "
                + currency.format(order.getTotalCents() / 100.0) + "</strong></p>"
                + "</body></html>";
    }

    private String buildStatusUpdateHtml(Order order) {
        String statusLabel = formatStatus(order.getStatus());
        return "<html><body style='font-family:Arial,sans-serif;color:#333;max-width:600px;margin:auto;'>"
                + "<h2 style='color:#e91e8c;'>Order Update</h2>"
                + "<p>Hi " + order.getFirstName() + ",</p>"
                + "<p>Your order <strong>#" + order.getId() + "</strong> status has been updated.</p>"
                + "<p style='font-size:1.1em;'>Status: <strong style='color:#e91e8c;'>" + statusLabel + "</strong></p>"
                + "<p>If you have any questions, feel free to reach out to us.</p>"
                + "<br/><p>Thank you for choosing Pink Cookie Jar! 🍪</p>"
                + "</body></html>";
    }

    private String formatStatus(String status) {
        if (status == null) return "";
        switch (status.toLowerCase()) {
            case "pending": return "Pending";
            case "confirmed": return "Confirmed";
            case "in_progress": return "In Progress";
            case "ready": return "Ready for Pickup";
            case "completed": return "Completed";
            case "cancelled": return "Cancelled";
            default: return status;
        }
    }
}
