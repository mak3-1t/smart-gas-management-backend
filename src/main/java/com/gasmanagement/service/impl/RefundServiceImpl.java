package com.gasmanagement.service.impl;

import com.gasmanagement.dto.request.RefundProcessRequest;
import com.gasmanagement.dto.response.RefundResponse;
import com.gasmanagement.exception.BusinessException;
import com.gasmanagement.exception.ResourceNotFoundException;
import com.gasmanagement.model.Order;
import com.gasmanagement.model.Refund;
import com.gasmanagement.model.enums.PaymentStatus;
import com.gasmanagement.model.enums.RefundStatus;
import com.gasmanagement.model.enums.UserRole;
import com.gasmanagement.repository.OrderRepository;
import com.gasmanagement.repository.RefundRepository;
import com.gasmanagement.service.AuditService;
import com.gasmanagement.service.RefundService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefundServiceImpl implements RefundService {

    private final RefundRepository refundRepository;
    private final OrderRepository orderRepository;
    private final AuditService auditService;

    @Override
    public List<RefundResponse> getAllRefunds(RefundStatus status) {
        List<Refund> refunds = status != null
                ? refundRepository.findByStatus(status)
                : refundRepository.findAll();
        return refunds.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    public RefundResponse getRefundById(String refundId) {
        Refund refund = refundRepository.findById(refundId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin hoàn tiền"));
        return mapToResponse(refund);
    }

    @Override
    public RefundResponse processRefund(String refundId, String managerId, RefundProcessRequest request) {
        Refund refund = refundRepository.findById(refundId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu hoàn tiền"));

        if (refund.getStatus() != RefundStatus.PENDING) {
            throw new BusinessException("Yêu cầu hoàn tiền này đã được xử lý trước đó");
        }

        LocalDateTime now = LocalDateTime.now();
        refund.setStatus(request.getStatus());
        refund.setProcessedBy(managerId);
        refund.setProcessedAt(now);
        refund.setNote(request.getNote());
        refund.setUpdatedAt(now);

        Refund savedRefund = refundRepository.save(refund);

        Order order = orderRepository.findById(refund.getOrderId()).orElse(null);
        if (order != null) {
            if (request.getStatus() == RefundStatus.PROCESSED) {
                order.setPaymentStatus(PaymentStatus.REFUNDED);
            } else {
                order.setPaymentStatus(PaymentStatus.PAID); // hoặc giữ PAID nếu từ chối hoàn tiền
            }
            order.setUpdatedAt(now);
            orderRepository.save(order);
        }

        log.info("Manager {} processed refund {}: status={}", managerId, refundId, request.getStatus());
        auditService.logAction(managerId, UserRole.MANAGER, "PROCESS_REFUND",
                "Refund", refundId, RefundStatus.PENDING, request.getStatus());

        return mapToResponse(savedRefund);
    }

    private RefundResponse mapToResponse(Refund r) {
        return RefundResponse.builder()
                .id(r.getId())
                .orderId(r.getOrderId())
                .paymentId(r.getPaymentId())
                .customerId(r.getCustomerId())
                .amount(r.getAmount())
                .reason(r.getReason())
                .status(r.getStatus())
                .processedBy(r.getProcessedBy())
                .processedAt(r.getProcessedAt())
                .note(r.getNote())
                .createdAt(r.getCreatedAt())
                .updatedAt(r.getUpdatedAt())
                .build();
    }
}
