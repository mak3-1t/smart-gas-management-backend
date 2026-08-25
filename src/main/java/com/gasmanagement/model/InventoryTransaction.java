package com.gasmanagement.model;

import com.gasmanagement.model.enums.InventoryTransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * Lịch sử mọi thay đổi tồn kho.
 * Không bao giờ chỉ update số mà không ghi log vào collection này.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "inventory_transactions")
public class InventoryTransaction {

    @Id
    private String id;

    @Indexed
    private String productId;

    /** Đơn hàng liên quan (nếu có) */
    @Indexed
    private String orderId;

    /** Loại giao dịch tồn kho */
    private InventoryTransactionType type;

    /**
     * Số lượng thay đổi.
     * Âm = xuất kho, Dương = nhập kho.
     */
    private int quantityChange;

    /** Loại bình: FULL | EMPTY | DAMAGED | RESERVED */
    private String cylinderType;

    private String note;

    /** Người thực hiện (staffId hoặc managerId) */
    private String createdBy;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
