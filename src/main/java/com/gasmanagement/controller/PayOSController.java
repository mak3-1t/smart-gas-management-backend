package com.gasmanagement.controller;

import com.gasmanagement.model.Order;
import com.gasmanagement.model.Payment;
import com.gasmanagement.model.enums.OrderStatus;
import com.gasmanagement.model.enums.PaymentStatus;
import com.gasmanagement.repository.OrderRepository;
import com.gasmanagement.repository.PaymentRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@RestController
@RequestMapping("/api/payos")
@RequiredArgsConstructor
public class PayOSController {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;

    @Value("${payos.client-id:}")
    private String clientId;

    @Value("${payos.api-key:}")
    private String apiKey;

    @Value("${payos.checksum-key:}")
    private String checksumKey;

    @Value("${payment.bank.bin:970418}")
    private String bankBin;

    @Value("${payment.bank.code:BIDV}")
    private String bankCode;

    @Value("${payment.bank.name:BIDV (Ngân hàng TMCP Đầu tư và Phát triển VN)}")
    private String bankName;

    @Value("${payment.bank.account-number:}")
    private String accountNumber;

    @Value("${payment.bank.account-name:}")
    private String accountName;

    private final RestTemplate restTemplate = new RestTemplate();

    private boolean isPayosConfigured() {
        return clientId != null && !clientId.isBlank()
                && apiKey != null && !apiKey.isBlank()
                && checksumKey != null && !checksumKey.isBlank();
    }

    /**
     * Lấy thông tin cấu hình công khai (KHÔNG BAO GIỜ trả về clientId, apiKey hay checksumKey ra Frontend)
     */
    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> getPayosConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("payosConfigured", isPayosConfigured());
        config.put("bankBin", bankBin);
        config.put("bankCode", bankCode);
        config.put("bankName", bankName);
        config.put("accountNumber", accountNumber);
        config.put("accountName", accountName);
        return ResponseEntity.ok(config);
    }

    /**
     * Tạo liên kết thanh toán PayOS (Payment Link) từ Backend.
     * Mọi khóa bí mật (x-client-id, x-api-key, checksumKey ký HMAC-SHA256) đều xử lý 100% ở Backend.
     */
    @PostMapping("/create-payment-link")
    public ResponseEntity<?> createPaymentLink(@RequestBody PayOSRequest request) {
        try {
            long orderCode = request.getOrderCode() > 0
                    ? request.getOrderCode()
                    : Long.parseLong(String.valueOf(System.currentTimeMillis()).substring(7));
            int amount = request.getAmount() > 0 ? request.getAmount() : 50000;
            String description = (request.getDescription() != null && !request.getDescription().isBlank())
                    ? request.getDescription()
                    : "GAS " + orderCode;

            if (description.length() > 25) {
                description = description.substring(0, 25);
            }

            String cancelUrl = request.getCancelUrl() != null ? request.getCancelUrl() : "http://localhost:3000/customer";
            String returnUrl = request.getReturnUrl() != null ? request.getReturnUrl() : "http://localhost:3000/customer";

            String signatureData = String.format("amount=%d&cancelUrl=%s&description=%s&orderCode=%d&returnUrl=%s",
                    amount, cancelUrl, description, orderCode, returnUrl);

            String signature = hmacSha256(signatureData, checksumKey);

            Map<String, Object> body = new HashMap<>();
            body.put("orderCode", orderCode);
            body.put("amount", amount);
            body.put("description", description);
            body.put("cancelUrl", cancelUrl);
            body.put("returnUrl", returnUrl);
            body.put("signature", signature);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-client-id", clientId);
            headers.set("x-api-key", apiKey);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    "https://api-merchant.payos.vn/v2/payment-requests",
                    entity,
                    String.class
            );

            log.info("PayOS Payment Link Created for orderCode {}", orderCode);
            return ResponseEntity.status(response.getStatusCode())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(response.getBody());

        } catch (Exception e) {
            log.error("Lỗi khi tạo PayOS payment link: ", e);
            Map<String, Object> fallback = new HashMap<>();
            fallback.put("code", "01");
            fallback.put("message", e.getMessage());
            fallback.put("bankBin", bankBin);
            fallback.put("bankCode", bankCode);
            fallback.put("bankName", bankName);
            fallback.put("accountNumber", accountNumber);
            fallback.put("accountName", accountName);
            return ResponseEntity.status(HttpStatus.OK).body(fallback);
        }
    }

    /**
     * Kiểm tra trạng thái thanh toán của orderCode trực tiếp từ PayOS thông qua Backend
     */
    @GetMapping("/status/{orderCode}")
    public ResponseEntity<?> getPaymentStatus(@PathVariable long orderCode) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-client-id", clientId);
            headers.set("x-api-key", apiKey);

            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    "https://api-merchant.payos.vn/v2/payment-requests/" + orderCode,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            return ResponseEntity.status(response.getStatusCode())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(response.getBody());
        } catch (Exception e) {
            Map<String, Object> err = new HashMap<>();
            err.put("code", "01");
            err.put("message", e.getMessage());
            return ResponseEntity.ok(err);
        }
    }

    /**
     * Webhook nhận thông báo thanh toán từ PayOS có xác thực chữ ký HMAC-SHA256 bằng checksumKey tại Backend
     */
    @PostMapping("/webhook")
    public ResponseEntity<Map<String, Object>> handlePayosWebhook(@RequestBody Map<String, Object> payload) {
        Map<String, Object> response = new HashMap<>();
        try {
            String receivedSignature = payload.get("signature") != null ? String.valueOf(payload.get("signature")) : "";
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) payload.get("data");

            if (data == null) {
                response.put("success", false);
                response.put("message", "Invalid webhook data");
                return ResponseEntity.badRequest().body(response);
            }

            // Sắp xếp key theo alphabet để tạo chuỗi kiểm tra chữ ký chuẩn PayOS
            TreeMap<String, Object> sortedData = new TreeMap<>(data);
            StringBuilder dataStr = new StringBuilder();
            for (Map.Entry<String, Object> entry : sortedData.entrySet()) {
                if (dataStr.length() > 0) {
                    dataStr.append("&");
                }
                Object val = entry.getValue();
                dataStr.append(entry.getKey()).append("=").append(val == null ? "" : String.valueOf(val));
            }

            String expectedSignature = hmacSha256(dataStr.toString(), checksumKey);
            if (!expectedSignature.equalsIgnoreCase(receivedSignature)) {
                log.warn("PayOS Webhook signature mismatch! Yêu cầu bị từ chối (không ghi log chữ ký để tránh lộ thông tin).");
                response.put("success", false);
                response.put("message", "Invalid signature");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            String orderCodeStr = String.valueOf(data.getOrDefault("orderCode", ""));
            String description = String.valueOf(data.getOrDefault("description", ""));
            markOrderPaidInternal(orderCodeStr, "PayOS Webhook verified (orderCode=" + orderCodeStr + ", desc=" + description + ")");

            response.put("success", true);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Lỗi xử lý PayOS webhook: ", e);
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Xác nhận đơn hàng đã thanh toán thành công qua PayOS / VietQR
     */
    @PostMapping("/confirm-order/{orderId}")
    public ResponseEntity<Map<String, Object>> confirmPayosOrderPayment(@PathVariable String orderId) {
        Order order = markOrderPaidInternal(orderId,
                "Thanh toán trực tuyến thành công qua PayOS (" + bankCode + " - " + accountNumber + ")");

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("orderId", order != null ? order.getId() : orderId);
        result.put("paymentStatus", "PAID");
        result.put("bankName", bankName);
        result.put("accountNumber", accountNumber);
        result.put("accountName", accountName);
        return ResponseEntity.ok(result);
    }

    private Order markOrderPaidInternal(String orderIdentifier, String note) {
        LocalDateTime now = LocalDateTime.now();
        Order order = orderRepository.findById(orderIdentifier)
                .orElseGet(() -> orderRepository.findByOrderCode(orderIdentifier).orElse(null));

        if (order != null) {
            order.setPaymentStatus(PaymentStatus.PAID);
            if (order.getOrderStatus() == OrderStatus.ORDER_PLACED) {
                order.setOrderStatus(OrderStatus.PAID);
            }
            if (order.getStatusHistory() == null) {
                order.setStatusHistory(new ArrayList<>());
            }
            order.getStatusHistory().add(Order.StatusHistory.builder()
                    .status(OrderStatus.PAID.name())
                    .changedBy(order.getCustomerId())
                    .note(note)
                    .changedAt(now)
                    .build());
            order.setUpdatedAt(now);
            orderRepository.save(order);

            Payment payment = paymentRepository.findByOrderId(order.getId()).orElse(null);
            if (payment != null) {
                payment.setPaymentStatus(PaymentStatus.PAID);
                payment.setPaidAmount(payment.getAmount());
                payment.setConfirmedAt(now);
                payment.setUpdatedAt(now);
                if (payment.getTransactions() == null) {
                    payment.setTransactions(new ArrayList<>());
                }
                payment.getTransactions().add(Payment.TransactionLog.builder()
                        .action("PAYOS_CONFIRMED")
                        .actorId(order.getCustomerId())
                        .note("Xác nhận thanh toán PayOS vào TK " + accountNumber + " (" + accountName + ")")
                        .createdAt(now)
                        .build());
                paymentRepository.save(payment);
            }
        }
        return order;
    }

    private String hmacSha256(String data, String key) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] hmacBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hmacBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Lỗi tính HMAC-SHA256: " + e.getMessage(), e);
        }
    }

    @Data
    public static class PayOSRequest {
        private long orderCode;
        private int amount;
        private String description;
        private String cancelUrl;
        private String returnUrl;
    }
}
