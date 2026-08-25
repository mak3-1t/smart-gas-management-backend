package com.gasmanagement.service;

import com.gasmanagement.dto.response.dashboard.DashboardSummaryResponse;
import com.gasmanagement.dto.response.dashboard.LowStockStats;
import com.gasmanagement.dto.response.dashboard.ProductSalesStats;
import com.gasmanagement.dto.response.dashboard.StaffPerformanceStats;

import java.util.List;

/**
 * Service cung cấp dữ liệu thống kê cho Dashboard Manager.
 *
 * <p>Revenue Rule: chỉ tính đơn {@code OrderStatus=DELIVERED AND PaymentStatus=PAID}
 */
public interface DashboardService {

    /**
     * Lấy toàn bộ số liệu Dashboard trong một lần gọi.
     * Dùng cho màn hình Dashboard chính của Manager.
     *
     * @return {@link DashboardSummaryResponse} chứa tất cả metric
     */
    DashboardSummaryResponse getDashboardSummary();

    /**
     * Top N sản phẩm bán chạy nhất (theo số lượng, chỉ DELIVERED+PAID).
     *
     * @param limit số lượng kết quả, mặc định 10
     */
    List<ProductSalesStats> getTopSellingProducts(int limit);

    /**
     * Top N sản phẩm bán chậm nhất (đã bán ít nhất 1 lần).
     *
     * @param limit số lượng kết quả, mặc định 10
     */
    List<ProductSalesStats> getSlowSellingProducts(int limit);

    /**
     * Doanh thu theo từng sản phẩm (top N theo revenue, DELIVERED+PAID).
     *
     * @param limit số lượng kết quả, mặc định 10
     */
    List<ProductSalesStats> getRevenueByProduct(int limit);

    /**
     * Hiệu suất giao hàng của tất cả Staff, sort theo successRate DESC.
     */
    List<StaffPerformanceStats> getStaffPerformance();

    /**
     * Danh sách sản phẩm có tồn kho thấp (availableCylinder < threshold).
     *
     * @param threshold ngưỡng cảnh báo (mặc định 5 bình)
     */
    List<LowStockStats> getLowStockProducts(int threshold);
}
