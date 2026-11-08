package com.gasmanagement.dto.response;

import com.gasmanagement.model.Order;
import com.gasmanagement.model.enums.*;
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
public class OrderResponse {

    private String id;
    private String orderCode;
    private String customerId;
    private String customerName;
    private String customerPhone;

    private OrderStatus orderStatus;
    private ApprovalStatus approvalStatus;
    private PaymentStatus paymentStatus;
    private PaymentMethod paymentMethod;
    private DeliveryType deliveryType;
    private LocalDateTime scheduledDeliveryTime;

    private Order.AddressSnapshot deliveryAddressSnapshot;
    private List<Order.OrderItem> items;

    private String voucherCode;
    private Double gasSubtotal;
    private Double cylinderFeeTotal;
    private Double deliveryFee;
    private Double discountAmount;
    private Double adjustmentFee;
    private String adjustmentNote;
    private Double finalAmount;

    private List<Order.StatusHistory> statusHistory;
    private List<Order.ApprovalHistory> approvalHistory;
    private String cancelReason;

    // Delivery info (if assigned)
    private String deliveryId;
    private DeliveryStatus deliveryStatus;
    private String staffId;
    private String staffName;
    private String staffPhone;

    // Payment info
    private String paymentId;
    private String qrCodeUrl;
    private String bankAccount;
    private String transferContent;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
