package com.cookiejar.service;

import com.cookiejar.model.Order;
import com.cookiejar.model.OrderItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.text.NumberFormat;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final String adminEmail;
    private final String fromEmail;

    public EmailService(@Autowired(required = false) JavaMailSender mailSender,
                        @Value("${app.admin-email:}") String adminEmail,
                        @Value("${spring.mail.username:}") String fromEmail) {
        this.mailSender = mailSender;
        this.adminEmail = adminEmail;
        this.fromEmail = fromEmail;
    }

    public void sendPaymentResubmissionNotification(Order order) {
        if (mailSender == null) { System.out.println("[Email] Mail not configured, skipping resubmission notification."); return; }
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(adminEmail);
            helper.setSubject("Payment Resubmitted – " + formatOrderNumber(order) + " from " + order.getFirstName() + " " + order.getLastName());
            helper.setText(buildResubmissionHtml(order), true);
            mailSender.send(msg);
        } catch (Exception e) {
            System.err.println("Failed to send resubmission email: " + e.getMessage());
        }
    }

    private String buildResubmissionHtml(Order order) {
        NumberFormat currency = NumberFormat.getCurrencyInstance(new Locale("en", "PH"));
        String proofSection = "";
        if (order.getProofOfPaymentUrl() != null && !order.getProofOfPaymentUrl().isBlank()) {
            proofSection = "<hr style='border:none;border-top:1px solid #eee;'/>"
                    + "<h3>New Proof of Payment</h3>"
                    + "<a href='" + order.getProofOfPaymentUrl() + "' target='_blank'>"
                    + "<img src='" + order.getProofOfPaymentUrl() + "' alt='Proof of payment' "
                    + "style='max-width:100%;border-radius:10px;border:1px solid #eee;display:block;'/>"
                    + "</a>";
        }
        return "<html><body style='font-family:Arial,sans-serif;color:#333;max-width:600px;margin:auto;'>"
                + "<h2 style='color:#e91e8c;'>Payment Resubmitted ✅</h2>"
                + "<p>The customer has uploaded a new proof of payment for order <strong>" + formatOrderNumber(order) + "</strong>.</p>"
                + "<p><strong>Name:</strong> " + order.getFirstName() + " " + order.getLastName() + "</p>"
                + "<p><strong>Email:</strong> " + (order.getEmail() != null ? order.getEmail() : "—") + "</p>"
                + "<p><strong>Phone:</strong> " + (order.getPhone() != null ? order.getPhone() : "—") + "</p>"
                + "<p><strong>Total:</strong> " + currency.format(order.getTotalCents() / 100.0) + "</p>"
                + proofSection
                + "<br/><p>Please review the payment in the admin dashboard.</p>"
                + "</body></html>";
    }

    public void sendNewOrderNotification(Order order) {
        if (mailSender == null) { System.out.println("[Email] Mail not configured, skipping new order notification."); return; }
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(adminEmail);
            helper.setSubject("New Order " + formatOrderNumber(order) + " from " + order.getFirstName() + " " + order.getLastName());
            helper.setText(buildNewOrderHtml(order), true);
            mailSender.send(msg);
        } catch (Exception e) {
            System.err.println("Failed to send new order email: " + e.getMessage());
        }
    }

    public void sendStatusUpdateNotification(Order order) {
        if (mailSender == null) { System.out.println("[Email] Mail not configured, skipping status update notification."); return; }
        if (order.getEmail() == null || order.getEmail().isBlank()) return;
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(order.getEmail());
            helper.setSubject("Your Order " + formatOrderNumber(order) + " Status Update");
            helper.setText(buildStatusUpdateHtml(order), true);
            mailSender.send(msg);
        } catch (Exception e) {
            System.err.println("Failed to send status update email: " + e.getMessage());
        }
    }

    public void sendOrderConfirmationToCustomer(Order order) {
        if (mailSender == null) { System.out.println("[Email] Mail not configured, skipping order confirmation."); return; }
        if (order.getEmail() == null || order.getEmail().isBlank()) return;
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(order.getEmail());
            helper.setSubject("PCJ Order Confirmation – " + formatOrderNumber(order));
            helper.setText(buildOrderConfirmationHtml(order), true);
            mailSender.send(msg);
        } catch (Exception e) {
            System.err.println("Failed to send order confirmation email: " + e.getMessage());
        }
    }

    private String buildNewOrderHtml(Order order) {
        NumberFormat currency = NumberFormat.getCurrencyInstance(new Locale("en", "PH"));
        StringBuilder rows = new StringBuilder();
        for (OrderItem item : order.getItems()) {
            String displayName = item.getProduct().getName();
            if (item.getVariantName() != null && !item.getVariantName().isBlank()) {
                displayName += " (" + item.getVariantName() + ")";
            }
            rows.append("<tr>")
                .append("<td style='padding:6px 12px;border-bottom:1px solid #eee;'>").append(displayName).append("</td>")
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
                + "<p><strong>Order:</strong> " + formatOrderNumber(order) + "</p>"
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
        NumberFormat currency = NumberFormat.getCurrencyInstance(new Locale("en", "PH"));
        String status = order.getStatus();
        String name = order.getFirstName();
        String orderNum = formatOrderNumber(order);

        if ("confirmed".equalsIgnoreCase(status)) {
            return "<html><body style='font-family:Arial,sans-serif;color:#333;max-width:600px;margin:auto;'>"
                    + "<h2 style='color:#e91e8c;'>Order Confirmed! 😊</h2>"
                    + "<p>Hello " + name + "! 😊</p>"
                    + "<p>Payment of <strong>" + currency.format(order.getTotalCents() / 100.0) + "</strong> received. "
                    + "<p>We will update you once your order is ready for pick up. 🍪</p>"
                    + "<br/><p>Thanks,<br/><strong>Pink Cookie Jar</strong></p>"
                    + "</body></html>";
        }

        if ("ready".equalsIgnoreCase(status)) {
            return "<html><body style='font-family:Arial,sans-serif;color:#333;max-width:600px;margin:auto;'>"
                    + "<h2 style='color:#e91e8c;'>Your Order is Ready for Pickup! 🍪</h2>"
                    + "<p>Hi " + name + "! 👋</p>"
                    + "<p>Your order <strong>" + orderNum + "</strong> is ready! You can pick it up now or at your chosen pickup time.</p>"
                    + "<div style='background:#fce4f0;border-radius:10px;padding:16px 20px;margin:16px 0;'>"
                    + "<p style='margin:0 0 8px;'>📍 <strong>Pickup Location:</strong> Pink Cookie Jar, Acacia Estates Taguig.</p>"
                    + "<p style='margin:0 0 8px;'>👤 <strong>Contact name:</strong> Ana</p>"
                    + "<p style='margin:0;'>📞 <strong>Contact no.:</strong> 09175870108</p>"
                    + "</div>"
                    + "<p>Thank you and enjoy your order!</p>"
                    + "<p style='color:#666;font-size:0.9em;'><strong>Note:</strong> Choose thermal bag for Lalamove.</p>"
                    + "<br/><p>Thanks,<br/><strong>Pink Cookie Jar</strong></p>"
                    + "</body></html>";
        }

        if ("completed".equalsIgnoreCase(status)) {
            return "<html><body style='font-family:Arial,sans-serif;color:#333;max-width:600px;margin:auto;'>"
                    + "<h2 style='color:#e91e8c;'>Order Completed! 🎉</h2>"
                    + "<p>Hi " + name + "! 🎉</p>"
                    + "<p>Your order <strong>" + orderNum + "</strong> has been marked as completed.</p>"
                    + "<p>We hope you enjoy every bite! 🍪 If you loved your order, we'd love to see you again.</p>"
                    + "<p>Thank you so much for choosing Pink Cookie Jar!</p>"
                    + "<br/><p>With love,<br/><strong>Pink Cookie Jar</strong></p>"
                    + "</body></html>";
        }

        if ("cancelled".equalsIgnoreCase(status)) {
            return "<html><body style='font-family:Arial,sans-serif;color:#333;max-width:600px;margin:auto;'>"
                    + "<h2 style='color:#e91e8c;'>Order Cancelled</h2>"
                    + "<p>Hi " + name + ",</p>"
                    + "<p>We're sorry to let you know that your order <strong>" + orderNum + "</strong> has been cancelled.</p>"
                    + "<p>If you believe this is a mistake or have any concerns, please reach out to us directly and we'll be happy to assist you.</p>"
                    + "<div style='background:#fce4f0;border-radius:10px;padding:16px 20px;margin:16px 0;'>"
                    + "<p style='margin:0 0 8px;'>📞 <strong>Contact no.:</strong> 09175870108</p>"
                    + "<p style='margin:0;'>👤 <strong>Contact name:</strong> Ana</p>"
                    + "</div>"
                    + "<p>We hope to serve you again soon. 🍪</p>"
                    + "<br/><p>Thanks,<br/><strong>Pink Cookie Jar</strong></p>"
                    + "</body></html>";
        }

        // Fallback for any other status
        return "<html><body style='font-family:Arial,sans-serif;color:#333;max-width:600px;margin:auto;'>"
                + "<h2 style='color:#e91e8c;'>Order Update</h2>"
                + "<p>Hi " + name + ",</p>"
                + "<p>Your order <strong>" + orderNum + "</strong> has been updated.</p>"
                + "<p>Status: <strong style='color:#e91e8c;'>" + formatStatus(status) + "</strong></p>"
                + "<p>If you have any questions, feel free to reach out to us.</p>"
                + "<br/><p>Thanks,<br/><strong>Pink Cookie Jar</strong></p>"
                + "</body></html>";
    }

    private String buildOrderConfirmationHtml(Order order) {
        NumberFormat currency = NumberFormat.getCurrencyInstance(new Locale("en", "PH"));
        StringBuilder rows = new StringBuilder();
        for (OrderItem item : order.getItems()) {
            String displayName = item.getProduct().getName();
            if (item.getVariantName() != null && !item.getVariantName().isBlank()) {
                displayName += " (" + item.getVariantName() + ")";
            }
            rows.append("<tr>")
                .append("<td style='padding:6px 12px;border-bottom:1px solid #eee;'>").append(displayName).append("</td>")
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
                + "<h2 style='color:#e91e8c;'>Order Received!</h2>"
                + "<p>Hi " + order.getFirstName() + ",</p>"
                + "<p>We&apos;ve received your order and our team will review your payment shortly.</p>"
                + "<p><strong>Order:</strong> " + formatOrderNumber(order) + "</p>"
                + "<p><strong>Needed By:</strong> " + neededAt + "</p>"
                + "<hr style='border:none;border-top:1px solid #eee;'/>"
                + "<h3>Order Summary</h3>"
                + "<table style='border-collapse:collapse;width:100%;'>"
                + "<thead><tr style='background:#fce4f0;'>"
                + "<th style='padding:8px 12px;text-align:left;'>Product</th>"
                + "<th style='padding:8px 12px;text-align:center;'>Qty</th>"
                + "<th style='padding:8px 12px;text-align:right;'>Unit Price</th>"
                + "</tr></thead><tbody>" + rows + "</tbody></table>"
                + "<p style='font-size:1.1em;margin-top:12px;'><strong>Total: "
                + currency.format(order.getTotalCents() / 100.0) + "</strong></p>"
                + "<hr style='border:none;border-top:1px solid #eee;'/>"
                + "<p style='color:#666;font-size:0.9em;'>You will receive another email once your order status is updated. "
                + "If you have any questions, feel free to reach out to us.</p>"
                + "<br/><p>Thank you for your order! 🍪</p>"
                + "</body></html>";
    }

    private String formatOrderNumber(Order order) {
        String date = DateTimeFormatter.ofPattern("yyyyMMdd")
                .withZone(ZoneId.of("Asia/Manila"))
                .format(order.getCreatedAt());
        return String.format("ORD-%s-%s", date, order.getId());
    }

    public void sendRepaymentRequestEmail(Order order, String message) {
        if (mailSender == null) { System.out.println("[Email] Mail not configured, skipping repayment request."); return; }
        if (order.getEmail() == null || order.getEmail().isBlank()) return;
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(order.getEmail());
            helper.setSubject("Action Required: Repayment Request for " + formatOrderNumber(order));
            helper.setText(buildRepaymentRequestHtml(order, message), true);
            mailSender.send(msg);
        } catch (Exception e) {
            System.err.println("Failed to send repayment request email: " + e.getMessage());
            throw new RuntimeException("Failed to send repayment request email: " + e.getMessage(), e);
        }
    }

    private String buildRepaymentRequestHtml(Order order, String message) {
        NumberFormat currency = NumberFormat.getCurrencyInstance(new Locale("en", "PH"));
        String proofSection = "";
        if (order.getProofOfPaymentUrl() != null && !order.getProofOfPaymentUrl().isBlank()) {
            proofSection = "<hr style='border:none;border-top:1px solid #eee;margin:20px 0;'/>"
                    + "<h3 style='margin-bottom:8px;'>Your Submitted Payment</h3>"
                    + "<p style='color:#666;font-size:0.9em;margin-bottom:12px;'>Below is the payment you submitted for your reference:</p>"
                    + "<a href='" + order.getProofOfPaymentUrl() + "' target='_blank'>"
                    + "<img src='" + order.getProofOfPaymentUrl() + "' alt='Your payment proof' "
                    + "style='max-width:100%;border-radius:10px;border:1px solid #eee;display:block;'/>"
                    + "</a>";
        }
        return "<html><body style='font-family:Arial,sans-serif;color:#333;max-width:600px;margin:auto;'>"
                + "<h2 style='color:#e91e8c;'>Payment Action Required ⚠️</h2>"
                + "<p>Hi " + order.getFirstName() + ",</p>"
                + "<p>Our team has reviewed your payment for order <strong>" + formatOrderNumber(order) + "</strong> "
                + "(Total: <strong>" + currency.format(order.getTotalCents() / 100.0) + "</strong>) and we need your attention:</p>"
                + "<div style='background:#fff3cd;border:1px solid #ffc107;border-radius:10px;padding:16px 20px;margin:16px 0;'>"
                + "<p style='margin:0;color:#856404;'>" + message.replace("\n", "<br/>") + "</p>"
                + "</div>"
                + proofSection
                + "<p>Please click the button below to resubmit your payment:</p>"
                + "<div style='text-align:center;margin:24px 0;'>"
                + "<a href='https://pinkcookiejar.com/order/resubmit-payment/" + order.getId() + "' "
                + "style='background:#e91e8c;color:#fff;text-decoration:none;padding:12px 28px;"
                + "border-radius:8px;font-weight:bold;font-size:1em;display:inline-block;'>"
                + "Resubmit Payment</a>"
                + "</div>"
                + "<p style='color:#666;font-size:0.9em;'>If you have any questions, feel free to reach out to us directly.</p>"
                + "<div style='background:#fce4f0;border-radius:10px;padding:12px 16px;margin-top:16px;'>"
                + "<p style='margin:0 0 4px;font-size:0.9em;'>📞 <strong>09175870108</strong></p>"
                + "<p style='margin:0;font-size:0.9em;'>👤 <strong>Ana</strong> — Pink Cookie Jar</p>"
                + "</div>"
                + "<br/><p>Thanks,<br/><strong>Pink Cookie Jar</strong></p>"
                + "</body></html>";
    }

    private String formatStatus(String status) {
        if (status == null) return "";
        switch (status.toLowerCase()) {
            case "pending": return "Pending";
            case "confirmed": return "Confirmed";
            case "ready": return "Ready for Pickup";
            case "completed": return "Completed";
            case "cancelled": return "Cancelled";
            default: return status;
        }
    }
}
