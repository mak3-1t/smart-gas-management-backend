package com.gasmanagement.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "voucher_usages")
public class VoucherUsage {
    @Id
    private String id;
    private String voucherId;
    private String voucherCode;
    private String customerId;
    private String orderId;
    private LocalDateTime usedAt;
}
