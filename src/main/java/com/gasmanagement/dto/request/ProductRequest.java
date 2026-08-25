package com.gasmanagement.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.util.List;

@Data
public class ProductRequest {
    
    @NotBlank(message = "Tên sản phẩm không được trống")
    private String name;
    
    private String brandId;
    private String categoryId;
    
    private String gasType;
    
    @Min(value = 0, message = "Trọng lượng phải không âm")
    private Double weightKg;
    
    @Min(value = 0, message = "Giá không được âm")
    private Double gasPrice;
    
    @Min(value = 0, message = "Phí vỏ bình không được âm")
    private Double cylinderFee;
    
    private String description;
    
    private List<String> images;
}
