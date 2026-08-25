package com.gasmanagement.dto.request;

import com.gasmanagement.model.enums.CylinderCondition;
import com.gasmanagement.model.enums.DeliveryFailureReason;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request body cho các hành động của Staff trong quá trình giao hàng.
 * Dùng chung cho: accept, reject, start, complete, fail, confirm-payment.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryActionRequest {

    /** ID của Staff thực hiện hành động */
    private String staffId;

    /** Ghi chú */
    private String note;

    // ─── Dùng khi FAIL ───
    /** Lý do thất bại khi giao hàng */
    private DeliveryFailureReason failureReason;

    // ─── Dùng khi xử lý Cylinder Exchange ───
    /** Tình trạng vỏ bình thu hồi */
    private CylinderCondition cylinderCondition;

    // ─── Dùng khi có Adjustment Fee ───
    /** Phí phụ thu (vd: khách EXCHANGE nhưng không có vỏ) */
    private Double adjustmentFee;
    private String adjustmentNote;
}
