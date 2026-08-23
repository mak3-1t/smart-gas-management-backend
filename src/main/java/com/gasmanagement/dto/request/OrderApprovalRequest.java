package com.gasmanagement.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request body khi Manager Approve hoặc Reject một Order.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderApprovalRequest {

    /**
     * ID của Manager thực hiện hành động.
     * DEV 1 sẽ lấy từ SecurityContext, tạm thời nhận từ request.
     */
    @NotBlank(message = "managerId không được để trống")
    private String managerId;

    /**
     * Ghi chú của Manager (lý do reject, ghi chú approve...).
     * Bắt buộc khi Reject.
     */
    private String note;
}
