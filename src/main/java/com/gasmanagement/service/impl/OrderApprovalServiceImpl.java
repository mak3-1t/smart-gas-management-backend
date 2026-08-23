package com.gasmanagement.service.impl;

import com.gasmanagement.dto.request.OrderApprovalRequest;
import com.gasmanagement.dto.response.OrderResponse;
import com.gasmanagement.exception.ResourceNotFoundException;
import com.gasmanagement.model.Order;
import com.gasmanagement.model.enums.ApprovalStatus;
import com.gasmanagement.model.enums.OrderStatus;
import com.gasmanagement.model.enums.PaymentStatus;
import com.gasmanagement.repository.InventoryRepository;
import com.gasmanagement.repository.OrderRepository;
import com.gasmanagement.service.InventoryService;
import com.gasmanagement.service.OrderApprovalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation của OrderApprovalService.
 *
 * Business Rule (từ task assignment DEV 2):
 *  - Chỉ Manager mới có thể Approve/Reject
 *  - Chỉ PENDING_APPROVAL order mới được xử lý
 *  - Approve → reserveStock cho từng OrderItem
 *  - Reject + đã thanh toán → PaymentStatus = REFUND_PENDING
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderApprovalServiceImpl implements OrderApprovalService {

    private final OrderRepository orderRepository;
    private final InventoryRepository inventoryRepository;
    private final InventoryService inventoryService;

    // ─────────────── READ ───────────────

    @Override
    public List<OrderResponse> getPendingOrders() {
        return orderRepository
                .findByApprovalStatusOrderByCreatedAtAsc(ApprovalStatus.PENDING_APPROVAL)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public OrderResponse getOrderById(String orderId) {
        return mapToResponse(findOrderOrThrow(orderId));
    }

    @Override
    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<OrderResponse> getOrdersByApprovalStatus(String approvalStatus) {
        ApprovalStatus status;
        try {
            status = ApprovalStatus.valueOf(approvalStatus.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "ApprovalStatus không hợp lệ: " + approvalStatus +
                    ". Hợp lệ: PENDING_APPROVAL | APPROVED | REJECTED");
        }
        return orderRepository.findByApprovalStatus(status)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ─────────────── APPROVAL ACTIONS ───────────────

    @Override
    public OrderResponse approveOrder(String orderId, OrderApprovalRequest request) {
        Order order = findOrderOrThrow(orderId);

        // Validate trạng thái
        validatePendingApproval(order);

        // Kiểm tra tồn kho trước khi approve
        validateStockForAllItems(order);

        // Cập nhật ApprovalStatus
        order.setApprovalStatus(ApprovalStatus.APPROVED);
        order.setUpdatedAt(LocalDateTime.now());

        // Ghi ApprovalHistory
        addApprovalHistory(order, "APPROVED", request.getManagerId(), request.getNote());

        // Thêm StatusHistory (order vẫn ORDER_PLACED, chờ payment/delivery)
        addStatusHistory(order, order.getOrderStatus().name(),
                request.getManagerId(), "Manager approved order");

        orderRepository.save(order);

        // Reserve stock cho từng item sau khi save order thành công
        reserveStockForOrder(order, request.getManagerId());

        log.info("Order {} APPROVED by manager {}", orderId, request.getManagerId());
        return mapToResponse(order);
    }

    @Override
    public OrderResponse rejectOrder(String orderId, OrderApprovalRequest request) {
        Order order = findOrderOrThrow(orderId);

        // Validate trạng thái
        validatePendingApproval(order);

        // Ghi chú lý do reject là bắt buộc
        if (request.getNote() == null || request.getNote().isBlank()) {
            throw new IllegalArgumentException("Phải có lý do khi Reject đơn hàng");
        }

        // Cập nhật ApprovalStatus + OrderStatus
        order.setApprovalStatus(ApprovalStatus.REJECTED);
        order.setOrderStatus(OrderStatus.CANCELLED);
        order.setCancelReason(request.getNote());
        order.setUpdatedAt(LocalDateTime.now());

        // Nếu đã thanh toán → chuyển sang REFUND_PENDING
        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            order.setPaymentStatus(PaymentStatus.REFUND_PENDING);
            log.info("Order {} đã PAID, chuyển sang REFUND_PENDING", orderId);
        }

        // Ghi ApprovalHistory
        addApprovalHistory(order, "REJECTED", request.getManagerId(), request.getNote());

        // Ghi StatusHistory
        addStatusHistory(order, OrderStatus.CANCELLED.name(),
                request.getManagerId(), "Manager rejected: " + request.getNote());

        orderRepository.save(order);

        log.info("Order {} REJECTED by manager {}", orderId, request.getManagerId());
        return mapToResponse(order);
    }

    // ─────────────── PRIVATE HELPERS ───────────────

    private Order findOrderOrThrow(String orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy đơn hàng với id: " + orderId));
    }

    /**
     * Đảm bảo order đang ở trạng thái chờ duyệt.
     */
    private void validatePendingApproval(Order order) {
        if (order.getApprovalStatus() != ApprovalStatus.PENDING_APPROVAL) {
            throw new IllegalStateException(
                    "Đơn hàng " + order.getOrderCode() +
                    " không thể xử lý. Trạng thái hiện tại: " + order.getApprovalStatus() +
                    ". Chỉ có thể xử lý đơn ở trạng thái PENDING_APPROVAL.");
        }
    }

    /**
     * Kiểm tra tồn kho trước khi approve.
     * Ném exception nếu bất kỳ sản phẩm nào không đủ hàng.
     */
    private void validateStockForAllItems(Order order) {
        if (order.getItems() == null || order.getItems().isEmpty()) {
            throw new IllegalStateException("Đơn hàng không có sản phẩm nào");
        }

        for (Order.OrderItem item : order.getItems()) {
            inventoryRepository.findByProductId(item.getProductId())
                    .ifPresentOrElse(inventory -> {
                        int available = inventory.getFullCylinder() - inventory.getReservedCylinder();
                        if (available < item.getQuantity()) {
                            throw new IllegalStateException(
                                    "Sản phẩm '" + item.getProductName() + "' không đủ hàng. " +
                                    "Khả dụng: " + available + ", cần: " + item.getQuantity());
                        }
                    }, () -> {
                        throw new ResourceNotFoundException(
                                "Không tìm thấy tồn kho cho sản phẩm: " + item.getProductName());
                    });
        }
    }

    /**
     * Reserve stock cho toàn bộ items trong order.
     * Gọi sau khi order đã được save với APPROVED status.
     */
    private void reserveStockForOrder(Order order, String managerId) {
        for (Order.OrderItem item : order.getItems()) {
            try {
                inventoryService.reserveStock(
                        item.getProductId(),
                        item.getQuantity(),
                        order.getId(),
                        managerId
                );
                log.debug("Reserved {} units of product {} for order {}",
                        item.getQuantity(), item.getProductId(), order.getId());
            } catch (Exception e) {
                // Log lỗi nhưng không roll back approve - Manager cần xử lý thủ công
                log.error("Không thể reserve stock cho sản phẩm {}: {}",
                        item.getProductId(), e.getMessage());
            }
        }
    }

    private void addApprovalHistory(Order order, String status, String managerId, String note) {
        if (order.getApprovalHistory() == null) {
            order.setApprovalHistory(new ArrayList<>());
        }
        order.getApprovalHistory().add(
                Order.ApprovalHistory.builder()
                        .status(status)
                        .managerId(managerId)
                        .note(note)
                        .changedAt(LocalDateTime.now())
                        .build()
        );
    }

    private void addStatusHistory(Order order, String status, String changedBy, String note) {
        if (order.getStatusHistory() == null) {
            order.setStatusHistory(new ArrayList<>());
        }
        order.getStatusHistory().add(
                Order.StatusHistory.builder()
                        .status(status)
                        .changedBy(changedBy)
                        .note(note)
                        .changedAt(LocalDateTime.now())
                        .build()
        );
    }

    // ─────────────── MAPPER ───────────────

    private OrderResponse mapToResponse(Order order) {
        return OrderResponse.builder()
                .id(order.getId())
                .orderCode(order.getOrderCode())
                .customerId(order.getCustomerId())
                .orderStatus(order.getOrderStatus())
                .approvalStatus(order.getApprovalStatus())
                .paymentStatus(order.getPaymentStatus())
                .paymentMethod(order.getPaymentMethod())
                .deliveryType(order.getDeliveryType())
                .scheduledDeliveryTime(order.getScheduledDeliveryTime())
                .deliveryAddressSnapshot(order.getDeliveryAddressSnapshot())
                .items(order.getItems())
                .voucherCode(order.getVoucherCode())
                .gasSubtotal(order.getGasSubtotal())
                .cylinderFeeTotal(order.getCylinderFeeTotal())
                .deliveryFee(order.getDeliveryFee())
                .discountAmount(order.getDiscountAmount())
                .adjustmentFee(order.getAdjustmentFee())
                .adjustmentNote(order.getAdjustmentNote())
                .finalAmount(order.getFinalAmount())
                .cancelReason(order.getCancelReason())
                .statusHistory(order.getStatusHistory())
                .approvalHistory(order.getApprovalHistory())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }
}
