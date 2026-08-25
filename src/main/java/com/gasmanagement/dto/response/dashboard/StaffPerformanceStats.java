package com.gasmanagement.dto.response.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Thống kê hiệu suất giao hàng của từng Staff.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffPerformanceStats {

    private String staffId;
    private String username;
    private String email;

    private int totalDeliveries;
    private int successDeliveries;
    private int failedDeliveries;

    /** Tỉ lệ giao hàng thành công (%) */
    private double successRate;

    /** Trạng thái hiện tại: AVAILABLE | BUSY | OFFLINE */
    private String currentStatus;
}
