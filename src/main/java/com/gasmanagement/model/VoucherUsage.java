package com.gasmanagement.model;

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
@Document(collection = "voucher_usages")
public class VoucherUsage {

    @Id
    private String id;

    @Indexed
    private String voucherId;

    @Indexed
    private String customerId;

    @Indexed
    private String orderId;

    private LocalDateTime usedAt;
}
