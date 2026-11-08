package com.gasmanagement.dto.request;

import com.gasmanagement.model.enums.PurchaseType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddToCartRequest {

    @NotBlank(message = "Product ID không được để trống")
    private String productId;

    @NotNull(message = "Loại mua hàng (EXCHANGE / NEW_CYLINDER) không được để trống")
    private PurchaseType purchaseType;

    @Min(value = 1, message = "Số lượng phải lớn hơn 0")
    private int quantity;
}
