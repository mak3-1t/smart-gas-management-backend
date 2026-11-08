package com.gasmanagement.dto.response;

import com.gasmanagement.model.Payment;
import com.gasmanagement.model.enums.PaymentMethod;
import com.gasmanagement.model.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {

    private String id;
    private String orderId;
    private String customerId;
    private PaymentMethod paymentMethod;
    private PaymentStatus paymentStatus;
    private Double amount;
    private Double paidAmount;

    private String bankAccount;
    private String transferContent;
    private String qrCodeUrl;
    private String paymentProofUrl;

    private String confirmedByStaffId;
    private LocalDateTime confirmedAt;

    private String gatewayTransactionId;
    private LocalDateTime paymentExpiredAt;

    private List<Payment.TransactionLog> transactions;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
