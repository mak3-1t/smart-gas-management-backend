package com.gasmanagement.dto.response;

import com.gasmanagement.model.enums.PurchaseType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartResponse {

    private String id;
    private String userId;

    @Builder.Default
    private List<CartItemResponse> items = new ArrayList<>();

    private String voucherCode;
    private Double gasSubtotal;
    private Double cylinderFeeTotal;
    private Double subtotal;
    private Double discountAmount;
    private Double deliveryFee;
    private Double finalAmount;

    private boolean isFreeShipping;
    private Double freeShippingThreshold;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CartItemResponse {
        private String productId;
        private String productName;
        private String productImage;
        private String brandName;
        private Double weightKg;
        private PurchaseType purchaseType;
        private int quantity;
        private Double unitPrice;
        private Double cylinderFee;
        private Double itemGasSubtotal;
        private Double itemCylinderSubtotal;
        private Double itemTotal;
        private boolean inStock;
        private int availableStock;
    }
}
