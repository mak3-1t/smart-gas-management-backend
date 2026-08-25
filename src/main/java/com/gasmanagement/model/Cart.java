package com.gasmanagement.model;

import com.gasmanagement.model.enums.PurchaseType;
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
@Document(collection = "carts")
public class Cart {

    @Id
    private String id;

    @Indexed(unique = true)
    private String userId;

    private List<CartItem> items;

    private String voucherId;
    private String voucherCode;

    private LocalDateTime updatedAt;

    // ───── Embedded CartItem ─────
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CartItem {
        private String productId;
        private String productName;
        private PurchaseType purchaseType;
        private int quantity;
        private Double unitPrice;
        private Double cylinderFee;
    }
}
