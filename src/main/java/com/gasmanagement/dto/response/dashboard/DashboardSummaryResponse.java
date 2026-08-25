package com.gasmanagement.dto.response.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response tổng hợp toàn bộ Dashboard Manager.
 *
 * <p>Tất cả metric trong một lần gọi API duy nhất:
 * {@code GET /api/dashboard/summary}
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardSummaryResponse {

    // ─── REVENUE ───────────────────────────────────────────────────────
    /** Doanh thu chỉ tính đơn OrderStatus=DELIVERED + PaymentStatus=PAID */
    private RevenueStats revenue;

    // ─── ORDER COUNTS ───────────────────────────────────────────────────
    private long totalOrders;
    private long ordersPendingApproval;
    private long ordersPaid;
    private long ordersDelivered;
    private long ordersCancelled;

    // ─── DELIVERY ───────────────────────────────────────────────────────
    private long totalDeliveries;
    private long failedDeliveries;
    private long activeDeliveries;     // ACCEPTED + DELIVERING

    // ─── STAFF ──────────────────────────────────────────────────────────
    private long availableStaff;
    private long busyStaff;
    private long offlineStaff;

    // ─── PRODUCTS SOLD ──────────────────────────────────────────────────
    /** Tổng số bình đã giao thành công */
    private long totalProductsSold;

    // ─── TOP / SLOW SELLING ─────────────────────────────────────────────
    /** Top 10 sản phẩm bán chạy nhất (theo số lượng) */
    private List<ProductSalesStats> topSellingProducts;

    /** Top 10 sản phẩm bán chậm nhất (đã từng bán, sort ASC) */
    private List<ProductSalesStats> slowSellingProducts;

    /** Doanh thu theo từng sản phẩm (top 10 theo revenue) */
    private List<ProductSalesStats> revenueByProduct;

    // ─── STAFF PERFORMANCE ──────────────────────────────────────────────
    /** Hiệu suất tất cả Staff (sort theo successRate DESC) */
    private List<StaffPerformanceStats> staffPerformance;

    // ─── LOW STOCK ──────────────────────────────────────────────────────
    /** Sản phẩm có tồn kho available < threshold (mặc định 5) */
    private List<LowStockStats> lowStockProducts;
}
