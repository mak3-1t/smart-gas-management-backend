package com.gasmanagement.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request body để Manager phân công Staff giao đơn hàng.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryAssignRequest {

    @NotBlank(message = "orderId không được để trống")
    private String orderId;

    @NotBlank(message = "staffId không được để trống")
    private String staffId;

    /** Số thứ tự trong chuyến ghép (nếu có) */
    private Integer deliverySequence;

    /** batchId nếu ghép nhiều đơn cùng 1 chuyến */
    private String batchId;
}
