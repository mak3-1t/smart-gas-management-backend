package com.gasmanagement.dto.response.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Cảnh báo tồn kho thấp.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LowStockStats {

    private String productId;
    private String productName;
    private String brandId;

    /** Bình đầy hiện có */
    private int fullCylinder;

    /** Đã giữ cho đơn APPROVED */
    private int reservedCylinder;

    /**
     * Số lượng thực sự có thể bán.
     * {@code availableCylinder = fullCylinder - reservedCylinder}
     */
    private int availableCylinder;

    /** Vỏ rỗng thu hồi */
    private int emptyCylinder;

    /** Bình hỏng */
    private int damagedCylinder;
}
