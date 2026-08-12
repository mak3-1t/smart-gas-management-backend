package com.gasmanagement.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ProductRequest {
    @NotBlank(message = "Tên sản phẩm không được để trống")
    private String name;

    @NotBlank(message = "Thương hiệu không được để trống")
    private String brandId;

    @NotBlank(message = "Danh mục không được để trống")
    private String categoryId;

    private String gasType;

    private Double weightKg;

    @NotNull(message = "Giá gas không được để trống")
    @Min(value = 0, message = "Giá gas phải lớn hơn hoặc bằng 0")
    private Double gasPrice;

    @NotNull(message = "Phí vỏ bình không được để trống")
    @Min(value = 0, message = "Phí vỏ bình phải lớn hơn hoặc bằng 0")
    private Double cylinderFee;

    private String description;
}
