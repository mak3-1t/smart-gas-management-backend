package com.gasmanagement.dto.response.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Thống kê bán hàng theo sản phẩm.
 * Dùng cho: Top bán chạy, Top chậm, Revenue by Product.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductSalesStats {

    private String productId;
    private String productName;
    private String brandName;
    private String gasType;

    /** Tổng số lượng đã bán (tính trên đơn DELIVERED + PAID) */
    private long totalQuantitySold;

    /** Tổng doanh thu từ sản phẩm này */
    private double totalRevenue;
}
