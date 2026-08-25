package com.gasmanagement.dto.response;

import com.gasmanagement.model.enums.DiscountType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoucherResponse {
    private String id;
    private String code;
    private DiscountType discountType;
    private Double discountValue;
    private Double minimumOrder;
    private Double maximumDiscount;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Integer usageLimit;
    private int usedCount;
    private String status;
    private boolean isUsable; // Flag tính toán dựa trên ngày và số lượng
}
