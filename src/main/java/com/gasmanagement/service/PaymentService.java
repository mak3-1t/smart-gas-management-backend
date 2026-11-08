package com.gasmanagement.service;

import com.gasmanagement.dto.request.PaymentProofRequest;
import com.gasmanagement.dto.response.PaymentResponse;

public interface PaymentService {
    PaymentResponse getPaymentById(String paymentId);
    PaymentResponse getPaymentByOrderId(String orderId);
    PaymentResponse confirmPayment(String paymentId, String staffOrManagerId, String note);
    PaymentResponse uploadPaymentProof(String paymentId, String customerId, PaymentProofRequest request);
    PaymentResponse verifyPayment(String paymentId, String managerId, boolean approved, String note);
}
