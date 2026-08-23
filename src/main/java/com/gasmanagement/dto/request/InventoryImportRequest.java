package com.gasmanagement.dto.request;

import com.gasmanagement.model.enums.InventoryTransactionType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class InventoryImportRequest {

    @NotBlank(message = "productId không được để trống")
    private String productId;

    @NotNull
    @Min(value = 1, message = "Số lượng nhập phải >= 1")
    private Integer quantity;

    private String note;

    /** ID của người thực hiện (Manager) */
    private String createdBy;
}
