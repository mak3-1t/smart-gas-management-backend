package com.gasmanagement.dto.response.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Thống kê doanh thu theo kỳ.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RevenueStats {

    /** Tổng doanh thu (tất cả thời gian) */
    private double totalRevenue;

    /** Doanh thu hôm nay */
    private double revenueToday;

    /** Doanh thu tuần này (Thứ 2 → hiện tại) */
    private double revenueThisWeek;

    /** Doanh thu tháng này */
    private double revenueThisMonth;

    /** Số đơn hàng tạo ra doanh thu (DELIVERED + PAID) */
    private long revenueOrderCount;
}
