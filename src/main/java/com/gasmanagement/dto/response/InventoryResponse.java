package com.gasmanagement.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryResponse {

    private String id;
    private String productId;
    private String productName;

    /** Bình đầy sẵn có */
    private int fullCylinder;

    /** Bình đã được giữ cho các đơn đã approve */
    private int reservedCylinder;

    /** Bình khả dụng = fullCylinder - reservedCylinder */
    private int availableCylinder;

    /** Vỏ bình rỗng thu từ khách */
    private int emptyCylinder;

    /** Bình hỏng */
    private int damagedCylinder;

    /** Cảnh báo hàng thấp (availableCylinder <= 5) */
    private boolean lowStock;
}
