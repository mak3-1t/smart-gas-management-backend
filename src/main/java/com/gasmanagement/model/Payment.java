package com.gasmanagement.model;

import com.gasmanagement.model.enums.PaymentMethod;
import com.gasmanagement.model.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "payments")
public class Payment {

    @Id
    private String id;

    @Indexed(unique = true)
    private String orderId;

    @Indexed
    private String customerId;

    private PaymentMethod paymentMethod;

    @Indexed
    @Builder.Default
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    private Double amount;     // Số tiền cần thanh toán
    private Double paidAmount; // Số tiền đã thu thực tế

    // Bank Transfer
    private String bankAccount;
    private String transferContent;
    private String qrCodeUrl;
    private String paymentProofUrl;

    // COD - Staff xác nhận đã thu tiền mặt
    private String confirmedByStaffId;
    private LocalDateTime confirmedAt;

    // Online Payment Gateway
    private String gatewayTransactionId;

    // Timeout thanh toán
    private LocalDateTime paymentExpiredAt;

    private List<TransactionLog> transactions;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ───── Embedded ─────
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class TransactionLog {
        private String action;
        private String actorId;
        private String note;
        private LocalDateTime createdAt;
    }
}
