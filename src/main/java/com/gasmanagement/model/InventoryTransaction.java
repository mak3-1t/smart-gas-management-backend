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

    @Indexed
    private String orderId; // null nếu không liên quan đến Order

    private InventoryTransactionType type;

    /** Số lượng thay đổi (âm = xuất, dương = nhập) */
    private int quantityChange;

    /** FULL | EMPTY | DAMAGED | RESERVED */
    private String cylinderType;

    private String note;

    private String createdBy; // userId của người thực hiện

    @Indexed
    private LocalDateTime createdAt;
}
