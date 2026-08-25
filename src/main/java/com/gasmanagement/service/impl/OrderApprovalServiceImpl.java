package com.gasmanagement.service.impl;

import com.gasmanagement.exception.BusinessException;
import com.gasmanagement.exception.ResourceNotFoundException;
import com.gasmanagement.model.Order;
import com.gasmanagement.model.enums.ApprovalStatus;
import com.gasmanagement.model.enums.OrderStatus;
import com.gasmanagement.model.enums.PaymentStatus;
import com.gasmanagement.repository.OrderRepository;
import com.gasmanagement.service.DeliveryService;
import com.gasmanagement.service.OrderApprovalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderApprovalServiceImpl implements OrderApprovalService {

    private final OrderRepository orderRepository;
    private final DeliveryService deliveryService;

    @Override
    public void approveOrder(String orderId, String managerId, String note) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng"));

        if (order.getApprovalStatus() != ApprovalStatus.PENDING_APPROVAL) {
            throw new BusinessException("Đơn hàng không ở trạng thái chờ duyệt");
        }

        // Cập nhật ApprovalStatus
        order.setApprovalStatus(ApprovalStatus.APPROVED);
        addApprovalHistory(order, ApprovalStatus.APPROVED.name(), managerId, note);
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);

        log.info("Manager {} đã duyệt đơn hàng {}", managerId, orderId);

        // Kích hoạt Delivery Assignment (System tự tìm Staff)
        // Hiện tại DEV 2 sử dụng cơ chế Manager phân công thủ công qua DeliveryService.assignDelivery() 
        // Hoặc có background cronjob tự động quét các đơn APPROVED để xử lý.
        log.info("Đơn hàng {} đang chờ phân công giao hàng (WAITING)", orderId);
    }

    @Override
    public void rejectOrder(String orderId, String managerId, String note) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng"));

        if (order.getApprovalStatus() != ApprovalStatus.PENDING_APPROVAL) {
            throw new BusinessException("Đơn hàng không ở trạng thái chờ duyệt");
        }

        order.setApprovalStatus(ApprovalStatus.REJECTED);
        order.setOrderStatus(OrderStatus.CANCELLED);
        order.setCancelReason(note != null ? note : "Manager rejected");

        // Nếu khách đã trả tiền -> Cần Refund
        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            order.setPaymentStatus(PaymentStatus.REFUND_PENDING);
        }

        addApprovalHistory(order, ApprovalStatus.REJECTED.name(), managerId, note);
        addStatusHistory(order, OrderStatus.CANCELLED.name(), managerId, "Order Rejected: " + note);
        order.setUpdatedAt(LocalDateTime.now());

        orderRepository.save(order);
        log.info("Manager {} đã từ chối đơn hàng {}", managerId, orderId);
    }

    private void addApprovalHistory(Order order, String status, String managerId, String note) {
        if (order.getApprovalHistory() == null) {
            order.setApprovalHistory(new ArrayList<>());
        }
        order.getApprovalHistory().add(Order.ApprovalHistory.builder()
                .status(status)
                .managerId(managerId)
                .note(note)
                .changedAt(LocalDateTime.now())
                .build());
    }

    private void addStatusHistory(Order order, String status, String changedBy, String note) {
        if (order.getStatusHistory() == null) {
            order.setStatusHistory(new ArrayList<>());
        }
        order.getStatusHistory().add(Order.StatusHistory.builder()
                .status(status)
                .changedBy(changedBy)
                .note(note)
                .changedAt(LocalDateTime.now())
                .build());
    }
}
