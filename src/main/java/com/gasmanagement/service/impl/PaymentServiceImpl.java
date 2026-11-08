package com.gasmanagement.service.impl;

import com.gasmanagement.dto.request.PaymentProofRequest;
import com.gasmanagement.dto.response.PaymentResponse;
import com.gasmanagement.exception.BusinessException;
import com.gasmanagement.exception.ResourceNotFoundException;
import com.gasmanagement.model.Order;
import com.gasmanagement.model.Payment;
import com.gasmanagement.model.enums.OrderStatus;
import com.gasmanagement.model.enums.PaymentStatus;
import com.gasmanagement.model.enums.UserRole;
import com.gasmanagement.repository.OrderRepository;
import com.gasmanagement.repository.PaymentRepository;
import com.gasmanagement.service.AuditService;
import com.gasmanagement.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final AuditService auditService;

    @Override
    public PaymentResponse getPaymentById(String paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin thanh toán"));
        return mapToResponse(payment);
    }

    @Override
    public PaymentResponse getPaymentByOrderId(String orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thanh toán cho đơn hàng: " + orderId));
        return mapToResponse(payment);
    }

    @Override
    public PaymentResponse confirmPayment(String paymentId, String staffOrManagerId, String note) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin thanh toán"));

        if (payment.getPaymentStatus() == PaymentStatus.PAID) {
            throw new BusinessException("Đơn hàng này đã được xác nhận thanh toán trước đó");
        }

        LocalDateTime now = LocalDateTime.now();
        payment.setPaymentStatus(PaymentStatus.PAID);
        payment.setPaidAmount(payment.getAmount());
        payment.setConfirmedByStaffId(staffOrManagerId);
        payment.setConfirmedAt(now);
        payment.setUpdatedAt(now);

        if (payment.getTransactions() == null) {
            payment.setTransactions(new ArrayList<>());
        }
        payment.getTransactions().add(Payment.TransactionLog.builder()
                .action("CONFIRM_PAYMENT")
                .actorId(staffOrManagerId)
                .note(note != null ? note : "Xác nhận thu tiền thành công")
                .createdAt(now)
                .build());

        Payment savedPayment = paymentRepository.save(payment);

        // Cập nhật Order liên quan
        Order order = orderRepository.findById(payment.getOrderId()).orElse(null);
        if (order != null) {
            order.setPaymentStatus(PaymentStatus.PAID);
            // Nếu đơn hàng đang ở trạng thái ORDER_PLACED thì chuyển sang PAID
            if (order.getOrderStatus() == OrderStatus.ORDER_PLACED) {
                order.setOrderStatus(OrderStatus.PAID);
            }
            if (order.getStatusHistory() == null) {
                order.setStatusHistory(new ArrayList<>());
            }
            order.getStatusHistory().add(Order.StatusHistory.builder()
                    .status(OrderStatus.PAID.name())
                    .changedBy(staffOrManagerId)
                    .note("Xác nhận thanh toán hoàn tất: " + (note != null ? note : ""))
                    .changedAt(now)
                    .build());
            order.setUpdatedAt(now);
            orderRepository.save(order);
        }

        log.info("Payment {} confirmed by {}", paymentId, staffOrManagerId);
        auditService.logAction(staffOrManagerId, UserRole.STAFF, "CONFIRM_PAYMENT",
                "Payment", paymentId, PaymentStatus.PENDING, PaymentStatus.PAID);

        return mapToResponse(savedPayment);
    }

    @Override
    public PaymentResponse uploadPaymentProof(String paymentId, String customerId, PaymentProofRequest request) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin thanh toán"));

        if (!payment.getCustomerId().equals(customerId)) {
            throw new BusinessException("Bạn không có quyền cập nhật thanh toán này");
        }

        payment.setPaymentProofUrl(request.getPaymentProofUrl());
        payment.setPaymentStatus(PaymentStatus.VERIFYING);
        payment.setUpdatedAt(LocalDateTime.now());

        if (payment.getTransactions() == null) {
            payment.setTransactions(new ArrayList<>());
        }
        payment.getTransactions().add(Payment.TransactionLog.builder()
                .action("UPLOAD_PROOF")
                .actorId(customerId)
                .note("Khách hàng tải lên ảnh biên lai chuyển khoản")
                .createdAt(LocalDateTime.now())
                .build());

        Payment saved = paymentRepository.save(payment);

        // Cập nhật trạng thái thanh toán trên Order sang VERIFYING
        Order order = orderRepository.findById(payment.getOrderId()).orElse(null);
        if (order != null) {
            order.setPaymentStatus(PaymentStatus.VERIFYING);
            order.setUpdatedAt(LocalDateTime.now());
            orderRepository.save(order);
        }

        log.info("Customer {} uploaded payment proof for payment {}", customerId, paymentId);
        return mapToResponse(saved);
    }

    @Override
    public PaymentResponse verifyPayment(String paymentId, String managerId, boolean approved, String note) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin thanh toán"));

        LocalDateTime now = LocalDateTime.now();
        if (approved) {
            payment.setPaymentStatus(PaymentStatus.PAID);
            payment.setPaidAmount(payment.getAmount());
            payment.setConfirmedByStaffId(managerId);
            payment.setConfirmedAt(now);
        } else {
            payment.setPaymentStatus(PaymentStatus.FAILED);
        }
        payment.setUpdatedAt(now);

        if (payment.getTransactions() == null) {
            payment.setTransactions(new ArrayList<>());
        }
        payment.getTransactions().add(Payment.TransactionLog.builder()
                .action(approved ? "VERIFY_PAYMENT_SUCCESS" : "VERIFY_PAYMENT_REJECTED")
                .actorId(managerId)
                .note(note != null ? note : (approved ? "Manager xác nhận chuyển khoản hợp lệ" : "Manager từ chối biên lai"))
                .createdAt(now)
                .build());

        Payment saved = paymentRepository.save(payment);

        Order order = orderRepository.findById(payment.getOrderId()).orElse(null);
        if (order != null) {
            if (approved) {
                order.setPaymentStatus(PaymentStatus.PAID);
                if (order.getOrderStatus() == OrderStatus.ORDER_PLACED) {
                    order.setOrderStatus(OrderStatus.PAID);
                }
            } else {
                order.setPaymentStatus(PaymentStatus.FAILED);
            }
            order.setUpdatedAt(now);
            orderRepository.save(order);
        }

        log.info("Manager {} verified payment {}: approved={}", managerId, paymentId, approved);
        auditService.logAction(managerId, UserRole.MANAGER, "VERIFY_PAYMENT",
                "Payment", paymentId, PaymentStatus.VERIFYING, payment.getPaymentStatus());

        return mapToResponse(saved);
    }

    private PaymentResponse mapToResponse(Payment p) {
        return PaymentResponse.builder()
                .id(p.getId())
                .orderId(p.getOrderId())
                .customerId(p.getCustomerId())
                .paymentMethod(p.getPaymentMethod())
                .paymentStatus(p.getPaymentStatus())
                .amount(p.getAmount())
                .paidAmount(p.getPaidAmount())
                .bankAccount(p.getBankAccount())
                .transferContent(p.getTransferContent())
                .qrCodeUrl(p.getQrCodeUrl())
                .paymentProofUrl(p.getPaymentProofUrl())
                .confirmedByStaffId(p.getConfirmedByStaffId())
                .confirmedAt(p.getConfirmedAt())
                .gatewayTransactionId(p.getGatewayTransactionId())
                .paymentExpiredAt(p.getPaymentExpiredAt())
                .transactions(p.getTransactions())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }
}
