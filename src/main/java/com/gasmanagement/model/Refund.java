package com.gasmanagement.model;

import com.gasmanagement.model.enums.RefundStatus;
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
@Document(collection = "refunds")
public class Refund {

    @Id
    private String id;

    @Indexed
    private String orderId;

    @Indexed
    private String paymentId;

    @Indexed
    private String customerId;

    private Double amount;
    private String reason;

    @Builder.Default
    private RefundStatus status = RefundStatus.PENDING;

    private String processedBy;
    private LocalDateTime processedAt;
    private String note;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
