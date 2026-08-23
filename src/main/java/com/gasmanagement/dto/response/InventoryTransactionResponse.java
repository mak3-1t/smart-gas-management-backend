package com.gasmanagement.dto.response;

import com.gasmanagement.model.enums.InventoryTransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryTransactionResponse {

    private String id;
    private String productId;
    private String productName;
    private String orderId;
    private InventoryTransactionType type;
    private int quantityChange;
    private String cylinderType;
    private String note;
    private String createdBy;
    private LocalDateTime createdAt;
}
