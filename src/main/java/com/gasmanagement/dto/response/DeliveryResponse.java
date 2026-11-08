package com.gasmanagement.dto.response;

import com.gasmanagement.model.Delivery;
import com.gasmanagement.model.Order;
import com.gasmanagement.model.enums.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO cho Delivery entity (đã đính kèm thông tin Order cho Shipper).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryResponse {

    private String id;
    private String orderId;
    private String orderCode;
    private String staffId;
    private String staffName;

    private String customerName;
    private String customerPhone;
    private Order.AddressSnapshot deliveryAddressSnapshot;
    private List<Order.OrderItem> items;

    private OrderStatus orderStatus;
    private ApprovalStatus approvalStatus;
    private PaymentMethod paymentMethod;
    private PaymentStatus paymentStatus;
    private String paymentId;
    private Double finalAmount;

    private DeliveryStatus deliveryStatus;

    private String batchId;
    private Integer deliverySequence;

    private DeliveryFailureReason failureReason;
    private String failureNote;

    private LocalDateTime assignedAt;
    private LocalDateTime acceptedAt;
    private LocalDateTime startedAt;
    private LocalDateTime deliveredAt;

    private List<Delivery.StatusHistory> statusHistory;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
