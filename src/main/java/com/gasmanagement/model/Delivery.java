package com.gasmanagement.model;

import com.gasmanagement.model.enums.DeliveryFailureReason;
import com.gasmanagement.model.enums.DeliveryStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "deliveries")
public class Delivery {

    @Id
    private String id;

    @Indexed
    private String orderId;

    /** Staff được phân công - có thể có nhiều Delivery active cùng lúc (Batch) */
    @Indexed
    private String staffId;

    @Indexed
    @Builder.Default
    private DeliveryStatus deliveryStatus = DeliveryStatus.WAITING;

    /** Batch/Route Delivery: ID chuyến ghép nếu nhiều đơn cùng lúc */
    @Indexed
    private String batchId;

    /** Thứ tự giao trong chuyến ghép */
    private Integer deliverySequence;

    private DeliveryFailureReason failureReason;
    private String failureNote;

    private LocalDateTime assignedAt;
    private LocalDateTime acceptedAt;
    private LocalDateTime startedAt;
    private LocalDateTime deliveredAt;

    private List<StatusHistory> statusHistory;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ───── Embedded ─────
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class StatusHistory {
        private String status;
        private String note;
        private LocalDateTime changedAt;
    }
}
