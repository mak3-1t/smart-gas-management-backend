package com.gasmanagement.model;

import com.gasmanagement.model.enums.DiscountType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "vouchers")
public class Voucher {

    @Id
    private String id;

    @Indexed(unique = true)
    private String code;

    private DiscountType discountType;
    private Double discountValue;
    private Double minimumOrder;
    private Double maximumDiscount;

    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Integer usageLimit;

    @Builder.Default
    private int usedCount = 0;

    @Builder.Default
    private String status = "ACTIVE"; // ACTIVE, INACTIVE, EXPIRED
}
