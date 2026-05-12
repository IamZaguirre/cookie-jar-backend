package com.cookiejar.service;

import com.cookiejar.model.Order;
import com.cookiejar.model.OrderItem;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.NumberFormat;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
public class EmailService {

    private final String apiKey;
    private final String fromEmail;
    private final String adminEmail;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public EmailService(@Value("${resend.api-key:}") String apiKey,
                        @Value("${resend.from-email:onboarding@resend.dev}") String fromEmail,
                        @Value("${app.admin-email:}") String adminEmail) {
        this.apiKey = apiKey;
        this.fromEmail = fromEmail;
        this.adminEmail = adminEmail;
        System.out.println("[Email] EmailService initialized. apiKey=" + (apiKey.isBlank() ? "MISSING" : "configured")
                + " fromEmail=" + (fromEmail.isBlank() ? "MISSING" : fromEmail)
                + " adminEmail=" + (adminEmail.isBlank() ? "MISSING" : adminEmail));
    }

    private void send(String to, String subject, String html) {
        if (apiKey.isBlank()) { System.out.println("[Email] RESEND_API_KEY not configured, skipping email to " + to); return; }
        System.out.println("[Email] Sending to=" + to + " subject=\"" + subject + "\"");
        try {
            String body = "{\"from\":\"" + escape(fromEmail) + "\","
                    + "\"to\":[\"" + escape(to) + "\"],"
                    + "\"subject\":\"" + escape(subject) + "\","
                    + "\"html\":" + jsonString(html) + "}";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.resend.com/emails"))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                System.out.println("[Email] Sent OK to " + to);
            } else {
                System.err.println("[Email] Resend returned " + response.statusCode() + " for " + to + ": " + response.body());
            }
        } catch (Exception e) {
            System.err.println("[Email] Failed to send to " + to + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }

    private static String jsonString(String s) {
        if (s == null) return "\"\"";
        StringBuilder sb = new StringBuilder("\"");
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '"') sb.append("\\\"");
            else if (c == '\\') sb.append("\\\\");
            else if (c == '\n') sb.append("\\n");
            else if (c == '\r') sb.append("\\r");
            else if (c == '\t') sb.append("\\t");
            else sb.append(c);
        }
        sb.append("\"");
        return sb.toString();
    }

    @Async
    public void sendNewOrderNotification(Order order) {
        send(adminEmail,
                "New Order " + formatOrderNumber(order) + " from " + order.getFirstName() + " " + order.getLastName(),
                buildNewOrderHtml(order));
    }

    @Async
    public void sendOrderConfirmationToCustomer(Order order) {
        if (order.getEmail() == null || order.getEmail().isBlank()) {
            System.out.println("[Email] Skipping order confirmation for order " + order.getId() + ": no customer email.");
            return;
        }
        send(order.getEmail(), "PCJ Order Confirmation - " + formatOrderNumber(order), buildOrderConfirmationHtml(order));
    }

    @Async
    public void sendStatusUpdateNotification(Order order) {
        if (order.getEmail() == null || order.getEmail().isBlank()) {
            System.out.println("[Email] Skipping status update for order " + order.getId() + ": no customer email.");
            return;
        }
        send(order.getEmail(), "Your Order " + formatOrderNumber(order) + " Status Update", buildStatusUpdateHtml(order));
    }

    @Async
    public void sendPaymentResubmissionNotification(Order order) {
        send(adminEmail,
                "Payment Resubmitted - " + formatOrderNumber(order) + " from " + order.getFirstName() + " " + order.getLastName(),
                buildResubmissionHtml(order));
    }

    public void sendRepaymentRequestEmail(Order order, String message) {
        if (apiKey.isBlank()) { throw new RuntimeException("RESEND_API_KEY not configured"); }
        if (order.getEmail() == null || order.getEmail().isBlank()) return;
        System.out.println("[Email] Sending repayment request for order " + order.getId() + " to " + order.getEmail());
        try {
            String body = "{\"from\":\"" + escape(fromEmail) + "\","
                    + "\"to\":[\"" + escape(order.getEmail()) + "\"],"
                    + "\"subject\":\"" + escape("Action Required: Repayment Request for " + formatOrderNumber(order)) + "\","
                    + "\"html\":" + jsonString(buildRepaymentRequestHtml(order, message)) + "}";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.resend.com/emails"))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new RuntimeException("Resend returned " + response.statusCode() + ": " + response.body());
            }
            System.out.println("[Email] Repayment request sent OK for order " + order.getId());
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            System.err.println("[Email] Failed to send repayment request for order " + order.getId() + ": " + e.getMessage());
            throw new RuntimeException("Failed to send repayment request email: " + e.getMessage(), e);
        }
    }

    private String buildNewOrderHtml(Order order) {
        NumberFormat currency = NumberFormat.getCurrencyInstance(new Locale("en", "PH"));
        StringBuilder rows = new StringBuilder();
        for (OrderItem item : order.getItems()) {
            String displayName = item.getProduct().getName();
            if (item.getVariantName() != null && !item.getVariantName().isBlank()) displayName += " (" + item.getVariantName() + ")";
            String unitPriceDisplay = currency.format(item.getUnitPrice() / 100.0);
            if (item.getAddOnTotalCents() != null && item.getAddOnTotalCents() > 0) {
                unitPriceDisplay += "<br><span style='font-size:12px;color:#666;'>+ add-ons " + currency.format(item.getAddOnTotalCents() / 100.0) + "</span>";
            }
            if (item.getCardMessage() != null && !item.getCardMessage().isBlank()) {
                displayName += "<br><span style='font-size:12px;color:#e91e8c;font-style:italic;'>Card: &ldquo;" + item.getCardMessage() + "&rdquo;</span>";
            }
            rows.append("<tr>")
                .append("<td style='padding:6px 12px;border-bottom:1px solid #eee;'>").append(displayName).append("</td>")
                .append("<td style='padding:6px 12px;text-align:center;border-bottom:1px solid #eee;'>").append(item.getQuantity()).append("</td>")
                .append("<td style='padding:6px 12px;text-align:right;border-bottom:1px solid #eee;'>").append(unitPriceDisplay).append("</td>")
                .append("</tr>");
        }
        String neededAt = order.getNeededAt() != null
                ? DateTimeFormatter.ofPattern("MMM d, yyyy hh:mm a").withZone(ZoneId.of("Asia/Manila")).format(order.getNeededAt())
                : "Not specified";
        return "<html><body style='font-family:Arial,sans-serif;color:#333;max-width:600px;margin:auto;'>"
                + "<h2 style='color:#e91e8c;'>New Order Received!</h2>"
                + "<p><strong>Order:</strong> " + formatOrderNumber(order) + "</p>"
                + "<hr style='border:none;border-top:1px solid #eee;'/>"
                + "<h3>Customer Details</h3>"
                + "<p><strong>Name:</strong> " + order.getFirstName() + " " + order.getLastName() + "</p>"
                + "<p><strong>Email:</strong> " + (order.getEmail() != null ? order.getEmail() : "-") + "</p>"
                + "<p><strong>Phone:</strong> " + (order.getPhone() != null ? order.getPhone() : "-") + "</p>"
                + "<p><strong>Needed By:</strong> " + neededAt + "</p>"
                + "<hr style='border:none;border-top:1px solid #eee;'/>"
                + "<h3>Items</h3>"
                + "<table style='border-collapse:collapse;width:100%;'>"
                + "<thead><tr style='background:#fce4f0;'>"
                + "<th style='padding:8px 12px;text-align:left;'>Product</th>"
                + "<th style='padding:8px 12px;text-align:center;'>Qty</th>"
                + "<th style='padding:8px 12px;text-align:right;'>Unit Price</th>"
                + "</tr></thead><tbody>" + rows + "</tbody></table>"
                + "<p style='font-size:1.1em;margin-top:12px;'><strong>Total: " + currency.format(order.getTotalCents() / 100.0) + "</strong></p>"
                + "</body></html>";
    }

    private String buildOrderConfirmationHtml(Order order) {
        NumberFormat currency = NumberFormat.getCurrencyInstance(new Locale("en", "PH"));
        StringBuilder rows = new StringBuilder();
        for (OrderItem item : order.getItems()) {
            String displayName = item.getProduct().getName();
            if (item.getVariantName() != null && !item.getVariantName().isBlank()) displayName += " (" + item.getVariantName() + ")";
            String unitPriceDisplay = currency.format(item.getUnitPrice() / 100.0);
            if (item.getAddOnTotalCents() != null && item.getAddOnTotalCents() > 0) {
                unitPriceDisplay += "<br><span style='font-size:12px;color:#666;'>+ add-ons " + currency.format(item.getAddOnTotalCents() / 100.0) + "</span>";
            }
            if (item.getCardMessage() != null && !item.getCardMessage().isBlank()) {
                displayName += "<br><span style='font-size:12px;color:#e91e8c;font-style:italic;'>Card: &ldquo;" + item.getCardMessage() + "&rdquo;</span>";
            }
            rows.append("<tr>")
                .append("<td style='padding:6px 12px;border-bottom:1px solid #eee;'>").append(displayName).append("</td>")
                .append("<td style='padding:6px 12px;text-align:center;border-bottom:1px solid #eee;'>").append(item.getQuantity()).append("</td>")
                .append("<td style='padding:6px 12px;text-align:right;border-bottom:1px solid #eee;'>").append(unitPriceDisplay).append("</td>")
                .append("</tr>");
        }
        String neededAt = order.getNeededAt() != null
                ? DateTimeFormatter.ofPattern("MMM d, yyyy hh:mm a").withZone(ZoneId.of("Asia/Manila")).format(order.getNeededAt())
                : "Not specified";
        return "<html><body style='font-family:Arial,sans-serif;color:#333;max-width:600px;margin:auto;'>"
                + "<h2 style='color:#e91e8c;'>Order Received!</h2>"
                + "<p>Hi " + order.getFirstName() + ",</p>"
                + "<p>We've received your order and our team will review your payment shortly.</p>"
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
                + "<p style='font-size:1.1em;margin-top:12px;'><strong>Total: " + currency.format(order.getTotalCents() / 100.0) + "</strong></p>"
                + "<hr style='border:none;border-top:1px solid #eee;'/>"
                + "<p style='color:#666;font-size:0.9em;'>You will receive another email once your order status is updated.</p>"
                + "<br/><p>Thank you for your order!</p>"
                + "</body></html>";
    }

    private String buildStatusUpdateHtml(Order order) {
        NumberFormat currency = NumberFormat.getCurrencyInstance(new Locale("en", "PH"));
        String status = order.getStatus();
        String name = order.getFirstName();
        String orderNum = formatOrderNumber(order);
        if ("confirmed".equalsIgnoreCase(status)) {
            return "<html><body style='font-family:Arial,sans-serif;color:#333;max-width:600px;margin:auto;'>"
                    + "<h2 style='color:#e91e8c;'>Order Confirmed!</h2>"
                    + "<p>Hello " + name + "!</p>"
                    + "<p>Payment of <strong>" + currency.format(order.getTotalCents() / 100.0) + "</strong> received.</p>"
                    + "<p>We will update you once your order is ready for pick up.</p>"
                    + "<br/><p>Thanks,<br/><strong>Pink Cookie Jar</strong></p>"
                    + "</body></html>";
        }
        if ("ready".equalsIgnoreCase(status)) {
            return "<html><body style='font-family:Arial,sans-serif;color:#333;max-width:600px;margin:auto;'>"
                    + "<h2 style='color:#e91e8c;'>Your Order is Ready for Pickup!</h2>"
                    + "<p>Hi " + name + "!</p>"
                    + "<p>Your order <strong>" + orderNum + "</strong> is ready!</p>"
                    + "<div style='background:#fce4f0;border-radius:10px;padding:16px 20px;margin:16px 0;'>"
                    + "<p style='margin:0 0 8px;'><strong>Pickup Location:</strong> Pink Cookie Jar, Acacia Estates Taguig.</p>"
                    + "<p style='margin:0 0 8px;'><strong>Contact name:</strong> Ana</p>"
                    + "<p style='margin:0;'><strong>Contact no.:</strong> 09175870108</p>"
                    + "</div>"
                    + "<p>Thank you and enjoy your order!</p>"
                    + "<p style='color:#666;font-size:0.9em;'><strong>Note:</strong> Choose thermal bag for Lalamove.</p>"
                    + "<br/><p>Thanks,<br/><strong>Pink Cookie Jar</strong></p>"
                    + "</body></html>";
        }
        if ("completed".equalsIgnoreCase(status)) {
            return "<html><body style='font-family:Arial,sans-serif;color:#333;max-width:600px;margin:auto;'>"
                    + "<h2 style='color:#e91e8c;'>Order Completed!</h2>"
                    + "<p>Hi " + name + "!</p>"
                    + "<p>Your order <strong>" + orderNum + "</strong> has been marked as completed.</p>"
                    + "<p>We hope you enjoy every bite! Thank you so much for choosing Pink Cookie Jar!</p>"
                    + "<br/><p>With love,<br/><strong>Pink Cookie Jar</strong></p>"
                    + "</body></html>";
        }
        if ("cancelled".equalsIgnoreCase(status)) {
            return "<html><body style='font-family:Arial,sans-serif;color:#333;max-width:600px;margin:auto;'>"
                    + "<h2 style='color:#e91e8c;'>Order Cancelled</h2>"
                    + "<p>Hi " + name + ",</p>"
                    + "<p>We're sorry to let you know that your order <strong>" + orderNum + "</strong> has been cancelled.</p>"
                    + "<div style='background:#fce4f0;border-radius:10px;padding:16px 20px;margin:16px 0;'>"
                    + "<p style='margin:0 0 8px;'><strong>Contact no.:</strong> 09175870108</p>"
                    + "<p style='margin:0;'><strong>Contact name:</strong> Ana</p>"
                    + "</div>"
                    + "<br/><p>Thanks,<br/><strong>Pink Cookie Jar</strong></p>"
                    + "</body></html>";
        }
        return "<html><body style='font-family:Arial,sans-serif;color:#333;max-width:600px;margin:auto;'>"
                + "<h2 style='color:#e91e8c;'>Order Update</h2>"
                + "<p>Hi " + name + ",</p>"
                + "<p>Your order <strong>" + orderNum + "</strong> status: <strong style='color:#e91e8c;'>" + formatStatus(status) + "</strong></p>"
                + "<br/><p>Thanks,<br/><strong>Pink Cookie Jar</strong></p>"
                + "</body></html>";
    }

    private String buildResubmissionHtml(Order order) {
        NumberFormat currency = NumberFormat.getCurrencyInstance(new Locale("en", "PH"));
        String proofSection = "";
        if (order.getProofOfPaymentUrl() != null && !order.getProofOfPaymentUrl().isBlank()) {
            proofSection = "<hr style='border:none;border-top:1px solid #eee;'/>"
                    + "<h3>New Proof of Payment</h3>"
                    + "<a href='" + order.getProofOfPaymentUrl() + "' target='_blank'>"
                    + "<img src='" + order.getProofOfPaymentUrl() + "' alt='Proof of payment' style='max-width:100%;border-radius:10px;border:1px solid #eee;display:block;'/>"
                    + "</a>";
        }
        return "<html><body style='font-family:Arial,sans-serif;color:#333;max-width:600px;margin:auto;'>"
                + "<h2 style='color:#e91e8c;'>Payment Resubmitted</h2>"
                + "<p>The customer has uploaded a new proof of payment for order <strong>" + formatOrderNumber(order) + "</strong>.</p>"
                + "<p><strong>Name:</strong> " + order.getFirstName() + " " + order.getLastName() + "</p>"
                + "<p><strong>Email:</strong> " + (order.getEmail() != null ? order.getEmail() : "-") + "</p>"
                + "<p><strong>Phone:</strong> " + (order.getPhone() != null ? order.getPhone() : "-") + "</p>"
                + "<p><strong>Total:</strong> " + currency.format(order.getTotalCents() / 100.0) + "</p>"
                + proofSection
                + "<br/><p>Please review the payment in the admin dashboard.</p>"
                + "</body></html>";
    }

    private String buildRepaymentRequestHtml(Order order, String message) {
        NumberFormat currency = NumberFormat.getCurrencyInstance(new Locale("en", "PH"));
        String proofSection = "";
        if (order.getProofOfPaymentUrl() != null && !order.getProofOfPaymentUrl().isBlank()) {
            proofSection = "<hr style='border:none;border-top:1px solid #eee;margin:20px 0;'/>"
                    + "<h3>Your Submitted Payment</h3>"
                    + "<a href='" + order.getProofOfPaymentUrl() + "' target='_blank'>"
                    + "<img src='" + order.getProofOfPaymentUrl() + "' alt='Your payment proof' style='max-width:100%;border-radius:10px;border:1px solid #eee;display:block;'/>"
                    + "</a>";
        }
        return "<html><body style='font-family:Arial,sans-serif;color:#333;max-width:600px;margin:auto;'>"
                + "<h2 style='color:#e91e8c;'>Payment Action Required</h2>"
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
                + "style='background:#e91e8c;color:#fff;text-decoration:none;padding:12px 28px;border-radius:8px;font-weight:bold;font-size:1em;display:inline-block;'>"
                + "Resubmit Payment</a>"
                + "</div>"
                + "<div style='background:#fce4f0;border-radius:10px;padding:12px 16px;margin-top:16px;'>"
                + "<p style='margin:0 0 4px;font-size:0.9em;'><strong>09175870108</strong></p>"
                + "<p style='margin:0;font-size:0.9em;'><strong>Ana</strong> - Pink Cookie Jar</p>"
                + "</div>"
                + "<br/><p>Thanks,<br/><strong>Pink Cookie Jar</strong></p>"
                + "</body></html>";
    }

    private String formatOrderNumber(Order order) {
        String date = DateTimeFormatter.ofPattern("yyyyMMdd").withZone(ZoneId.of("Asia/Manila")).format(order.getCreatedAt());
        return String.format("ORD-%s-%s", date, order.getId());
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