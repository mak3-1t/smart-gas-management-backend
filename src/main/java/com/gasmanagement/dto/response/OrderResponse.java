package com.gasmanagement.dto.response;

import com.gasmanagement.model.Order;
import com.gasmanagement.model.enums.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO cho Order – dùng trong Manager Order list và Approval flow.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {

    private String id;
    private String orderCode;
    private String customerId;

    private OrderStatus orderStatus;
    private ApprovalStatus approvalStatus;
    private PaymentStatus paymentStatus;
    private PaymentMethod paymentMethod;
    private DeliveryType deliveryType;

    private LocalDateTime scheduledDeliveryTime;

    /** Snapshot địa chỉ giao hàng tại thời điểm đặt */
    private Order.AddressSnapshot deliveryAddressSnapshot;

    /** Danh sách sản phẩm trong đơn (snapshot tại thời điểm đặt) */
    private List<Order.OrderItem> items;

    private String voucherCode;
    private Double gasSubtotal;
    private Double cylinderFeeTotal;
    private Double deliveryFee;
    private Double discountAmount;
    private Double adjustmentFee;
    private String adjustmentNote;
    private Double finalAmount;

    private String cancelReason;

    private List<Order.StatusHistory> statusHistory;
    private List<Order.ApprovalHistory> approvalHistory;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
