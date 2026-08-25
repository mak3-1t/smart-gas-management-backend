package com.gasmanagement.model;

import com.gasmanagement.model.enums.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "orders")
public class Order {

    @Id
    private String id;

    @Indexed(unique = true)
    private String orderCode; // VD: GAS-1024

    @Indexed
    private String customerId;

    @Indexed
    @Builder.Default
    private OrderStatus orderStatus = OrderStatus.ORDER_PLACED;

    @Indexed
    @Builder.Default
    private ApprovalStatus approvalStatus = ApprovalStatus.PENDING_APPROVAL;

    @Indexed
    @Builder.Default
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    private PaymentMethod paymentMethod;
    private DeliveryType deliveryType;
    private LocalDateTime scheduledDeliveryTime;

    /** Snapshot địa chỉ lúc đặt - KHÔNG thay đổi dù Customer sửa profile sau */
    private AddressSnapshot deliveryAddressSnapshot;

    /** OrderItems nhúng vào Order - lưu giá TẠI THỜI ĐIỂM ĐẶT */
    private List<OrderItem> items;

    private String voucherId;
    private String voucherCode;
    private Double gasSubtotal;
    private Double cylinderFeeTotal;
    private Double deliveryFee;
    private Double discountAmount;

    /** Phụ thu điều chỉnh tại điểm giao hàng (VD: khách không có vỏ) */
    private Double adjustmentFee;
    private String adjustmentNote;

    private Double finalAmount;

    private List<StatusHistory> statusHistory;
    private List<ApprovalHistory> approvalHistory;
    private String cancelReason;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ───── Embedded Classes ─────

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class AddressSnapshot {
        private String recipientName;
        private String phone;
        private String addressLine;
        private String ward;
        private String district;
        private String city;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class OrderItem {
        private String productId;
        private String productName;   // Snapshot
        private String brandName;     // Snapshot
        private Double weightKg;
        private PurchaseType purchaseType;
        private int quantity;

        /** Giá gas lúc đặt - KHÔNG thay đổi dù Manager đổi giá sau */
        private Double unitPriceAtOrderTime;
        private Double cylinderFeeAtOrderTime;

        private CylinderExchangeStatus cylinderExchangeStatus;
        private CylinderCondition cylinderCondition;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class StatusHistory {
        private String status;
        private String changedBy;
        private String note;
        private LocalDateTime changedAt;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ApprovalHistory {
        private String status;
        private String managerId;
        private String note;
        private LocalDateTime changedAt;
    }
}
