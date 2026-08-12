package com.gasmanagement.dto.response;

import com.gasmanagement.model.enums.ProductStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ProductResponse {
    private String id;
    private String name;
    private String brandId;
    private String categoryId;
    private String gasType;
    private Double weightKg;
    private Double gasPrice;
    private Double cylinderFee;
    private String description;
    private List<String> images;
    private ProductStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
