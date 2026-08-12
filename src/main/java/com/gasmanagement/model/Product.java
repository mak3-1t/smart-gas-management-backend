package com.gasmanagement.model;

import com.gasmanagement.model.enums.ProductStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.index.TextIndexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "products")
public class Product {

    @Id
    private String id;

    @TextIndexed
    private String name;

    private String brandId;
    private String categoryId;

    private String gasType;
    private Double weightKg;

    /** Giá gas - KHÔNG bao gồm vỏ bình */
    private Double gasPrice;

    /** Phí vỏ bình - chỉ tính khi PurchaseType = NEW_CYLINDER */
    private Double cylinderFee;

    @TextIndexed
    private String description;

    private List<String> images;

    @Indexed
    @Builder.Default
    private ProductStatus status = ProductStatus.ACTIVE;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
