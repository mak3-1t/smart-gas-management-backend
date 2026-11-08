package com.gasmanagement.dto.response;

import com.gasmanagement.model.enums.ProductStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponse {
    private String id;
    private String name;
    
    private String brandId;
    private String brandName;
    
    private String categoryId;
    private String categoryName;
    
    private String gasType;
    private Double weightKg;
    
    private Double gasPrice;
    private Double cylinderFee;
    
    private String description;
    private List<String> images;
    
    private ProductStatus status;

    private boolean inStock;
    private int availableStock;
}
