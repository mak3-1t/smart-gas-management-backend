package com.gasmanagement.controller;

import com.gasmanagement.dto.response.ApiResponse;
import com.gasmanagement.dto.response.dashboard.*;
import com.gasmanagement.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller cho Dashboard và Report của Manager.
 *
 * <p>Base path: {@code /api/dashboard}
 *
 * <p>Tất cả endpoint yêu cầu role {@code MANAGER}.
 */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasRole('MANAGER')")
public class DashboardController {

    private final DashboardService dashboardService;

    /**
     * Dashboard tổng hợp – tất cả metric trong 1 lần gọi.
     * Dùng cho màn hình Dashboard chính của Manager.
     *
     * <pre>GET /api/dashboard/summary</pre>
     */
    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<DashboardSummaryResponse>> getDashboardSummary() {
        return ResponseEntity.ok(ApiResponse.ok(dashboardService.getDashboardSummary()));
    }

    // ─────────────── REVENUE ───────────────

    /**
     * Chỉ lấy số liệu doanh thu (today / week / month / total).
     * Dùng cho widget doanh thu realtime.
     *
     * <pre>GET /api/dashboard/revenue</pre>
     */
    @GetMapping("/revenue")
    public ResponseEntity<ApiResponse<RevenueStats>> getRevenueStats() {
        DashboardSummaryResponse summary = dashboardService.getDashboardSummary();
        return ResponseEntity.ok(ApiResponse.ok(summary.getRevenue()));
    }

    // ─────────────── PRODUCTS ───────────────

    /**
     * Top N sản phẩm bán chạy nhất (mặc định 10).
     *
     * <pre>GET /api/dashboard/products/top-selling?limit=10</pre>
     */
    @GetMapping("/products/top-selling")
    public ResponseEntity<ApiResponse<List<ProductSalesStats>>> getTopSellingProducts(
            @RequestParam(defaultValue = "10") int limit) {

        return ResponseEntity.ok(
                ApiResponse.ok(dashboardService.getTopSellingProducts(limit)));
    }

    /**
     * Top N sản phẩm bán chậm nhất (mặc định 10).
     *
     * <pre>GET /api/dashboard/products/slow-selling?limit=10</pre>
     */
    @GetMapping("/products/slow-selling")
    public ResponseEntity<ApiResponse<List<ProductSalesStats>>> getSlowSellingProducts(
            @RequestParam(defaultValue = "10") int limit) {

        return ResponseEntity.ok(
                ApiResponse.ok(dashboardService.getSlowSellingProducts(limit)));
    }

    /**
     * Doanh thu theo từng sản phẩm (top N).
     *
     * <pre>GET /api/dashboard/products/revenue?limit=10</pre>
     */
    @GetMapping("/products/revenue")
    public ResponseEntity<ApiResponse<List<ProductSalesStats>>> getRevenueByProduct(
            @RequestParam(defaultValue = "10") int limit) {

        return ResponseEntity.ok(
                ApiResponse.ok(dashboardService.getRevenueByProduct(limit)));
    }

    // ─────────────── STAFF ───────────────

    /**
     * Hiệu suất giao hàng của tất cả Staff (sort successRate DESC).
     *
     * <pre>GET /api/dashboard/staff/performance</pre>
     */
    @GetMapping("/staff/performance")
    public ResponseEntity<ApiResponse<List<StaffPerformanceStats>>> getStaffPerformance() {
        return ResponseEntity.ok(
                ApiResponse.ok(dashboardService.getStaffPerformance()));
    }

    // ─────────────── INVENTORY ───────────────

    /**
     * Danh sách sản phẩm tồn kho thấp.
     *
     * <pre>GET /api/dashboard/inventory/low-stock?threshold=5</pre>
     *
     * @param threshold ngưỡng cảnh báo (mặc định 5 bình)
     */
    @GetMapping("/inventory/low-stock")
    public ResponseEntity<ApiResponse<List<LowStockStats>>> getLowStockProducts(
            @RequestParam(defaultValue = "5") int threshold) {

        return ResponseEntity.ok(
                ApiResponse.ok(dashboardService.getLowStockProducts(threshold)));
    }
}
