package com.gasmanagement.dto.response;

import com.gasmanagement.model.Delivery;
import com.gasmanagement.model.enums.DeliveryFailureReason;
import com.gasmanagement.model.enums.DeliveryStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO cho Delivery entity.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryResponse {

    private String id;
    private String orderId;
    private String staffId;
    private String staffName;

    private DeliveryStatus deliveryStatus;

    private String batchId;
    private Integer deliverySequence;

    private DeliveryFailureReason failureReason;
    private String failureNote;

    private LocalDateTime assignedAt;
    private LocalDateTime acceptedAt;
    private LocalDateTime startedAt;
    private LocalDateTime deliveredAt;

    private List<Delivery.StatusHistory> statusHistory;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
