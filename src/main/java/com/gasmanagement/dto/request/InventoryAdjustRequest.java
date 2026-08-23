package com.gasmanagement.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class InventoryAdjustRequest {

    @NotBlank(message = "productId không được để trống")
    private String productId;

    /**
     * Loại bình cần điều chỉnh: FULL | EMPTY | DAMAGED
     */
    @NotBlank(message = "cylinderType không được để trống")
    private String cylinderType;

    /**
     * Số lượng thay đổi (âm = giảm, dương = tăng)
     */
    @NotNull
    private Integer quantityChange;

    @NotBlank(message = "Lý do điều chỉnh không được để trống")
    private String note;

    /** ID của người thực hiện (Manager) */
    private String createdBy;
}
