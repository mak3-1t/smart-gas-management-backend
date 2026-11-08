package com.gasmanagement.dto.response;

import com.gasmanagement.model.enums.RefundStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundResponse {

    private String id;
    private String orderId;
    private String paymentId;
    private String customerId;
    private Double amount;
    private String reason;
    private RefundStatus status;
    private String processedBy;
    private LocalDateTime processedAt;
    private String note;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
