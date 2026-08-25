package com.gasmanagement.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "inventories")
public class Inventory {

    @Id
    private String id;

    @Indexed(unique = true)
    private String productId;

    /** Bình đầy có thể bán */
    @Builder.Default
    private int fullCylinder = 0;

    /** Vỏ bình rỗng thu hồi từ khách (Exchange) */
    @Builder.Default
    private int emptyCylinder = 0;

    /** Bình hỏng */
    @Builder.Default
    private int damagedCylinder = 0;

    /** Đã giữ cho đơn được Manager Approve - chờ giao */
    @Builder.Default
    private int reservedCylinder = 0;

    /** Thực tế có thể bán = fullCylinder - reservedCylinder */
    public int getAvailableCylinder() {
        return Math.max(0, fullCylinder - reservedCylinder);
    }
}
