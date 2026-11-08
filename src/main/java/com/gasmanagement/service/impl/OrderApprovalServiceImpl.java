package com.gasmanagement.service.impl;

import com.gasmanagement.exception.BusinessException;
import com.gasmanagement.exception.ResourceNotFoundException;
import com.gasmanagement.model.Order;
import com.gasmanagement.model.Payment;
import com.gasmanagement.model.Refund;
import com.gasmanagement.model.enums.ApprovalStatus;
import com.gasmanagement.model.enums.OrderStatus;
import com.gasmanagement.model.enums.PaymentStatus;
import com.gasmanagement.model.enums.RefundStatus;
import com.gasmanagement.model.enums.UserRole;
import com.gasmanagement.repository.OrderRepository;
import com.gasmanagement.repository.PaymentRepository;
import com.gasmanagement.repository.RefundRepository;
import com.gasmanagement.service.InventoryService;
import com.gasmanagement.service.NotificationService;
import com.gasmanagement.service.OrderApprovalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderApprovalServiceImpl implements OrderApprovalService {

    private final OrderRepository orderRepository;
    private final InventoryService inventoryService;
    private final PaymentRepository paymentRepository;
    private final RefundRepository refundRepository;
    private final NotificationService notificationService;

    @Override
    public void approveOrder(String orderId, String managerId, String note) {
        Order order = orderRepository.findById(orderId)
                .orElseGet(() -> orderRepository.findByOrderCode(orderId)
                        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng: " + orderId)));

        if (order.getApprovalStatus() != ApprovalStatus.PENDING_APPROVAL) {
            throw new BusinessException("Đơn hàng không ở trạng thái chờ duyệt");
        }

        // Khóa tồn kho (Reserve Stock) khi Manager duyệt đơn
        if (order.getItems() != null) {
            for (Order.OrderItem item : order.getItems()) {
                try {
                    inventoryService.reserveStock(item.getProductId(), item.getQuantity(), order.getId(), managerId);
                } catch (Exception e) {
                    log.warn("Không thể reserve tồn kho cho sản phẩm {}: {}", item.getProductId(), e.getMessage());
                }
            }
        }

        // Cập nhật ApprovalStatus
        order.setApprovalStatus(ApprovalStatus.APPROVED);
        addApprovalHistory(order, ApprovalStatus.APPROVED.name(), managerId, note);
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);

        if (order.getCustomerId() != null) {
            notificationService.sendNotification(
                    order.getCustomerId(),
                    UserRole.CUSTOMER,
                    "Đơn hàng " + order.getOrderCode() + " đã được duyệt",
                    "Cửa hàng đã xác nhận đơn hàng của bạn và đang điều phối nhân viên giao hàng.",
                    "ORDER_APPROVED",
                    order.getId()
            );
        }

        log.info("Manager {} đã duyệt đơn hàng {}", managerId, order.getId());
    }

    @Override
    public void rejectOrder(String orderId, String managerId, String note) {
        Order order = orderRepository.findById(orderId)
                .orElseGet(() -> orderRepository.findByOrderCode(orderId)
                        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng: " + orderId)));

        if (order.getApprovalStatus() != ApprovalStatus.PENDING_APPROVAL) {
            throw new BusinessException("Đơn hàng không ở trạng thái chờ duyệt");
        }

        LocalDateTime now = LocalDateTime.now();
        String rejectReason = note != null && !note.isBlank() ? note : "Manager từ chối đơn hàng";
        order.setApprovalStatus(ApprovalStatus.REJECTED);
        order.setOrderStatus(OrderStatus.CANCELLED);
        order.setCancelReason(rejectReason);

        // Nếu khách đã trả tiền -> Tạo bản ghi Refund và đưa vào REFUND_PENDING
        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            order.setPaymentStatus(PaymentStatus.REFUND_PENDING);
            Payment payment = paymentRepository.findByOrderId(order.getId()).orElse(null);
            Refund refund = Refund.builder()
                    .orderId(order.getId())
                    .paymentId(payment != null ? payment.getId() : null)
                    .customerId(order.getCustomerId())
                    .amount(order.getFinalAmount())
                    .reason(rejectReason)
                    .status(RefundStatus.PENDING)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();
            refundRepository.save(refund);
        }

        addApprovalHistory(order, ApprovalStatus.REJECTED.name(), managerId, rejectReason);
        addStatusHistory(order, OrderStatus.CANCELLED.name(), managerId, "Order Rejected: " + rejectReason);
        order.setUpdatedAt(now);

        orderRepository.save(order);

        if (order.getCustomerId() != null) {
            notificationService.sendNotification(
                    order.getCustomerId(),
                    UserRole.CUSTOMER,
                    "Đơn hàng " + order.getOrderCode() + " bị từ chối",
                    "Lý do: " + rejectReason,
                    "ORDER_REJECTED",
                    order.getId()
            );
        }

        log.info("Manager {} đã từ chối đơn hàng {}", managerId, order.getId());
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
