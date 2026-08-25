package com.gasmanagement.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Chuyến giao hàng ghép nhiều đơn (Batch/Route Delivery).
 * 1 Staff có thể nhận nhiều Delivery Job trong 1 chuyến.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "delivery_batches")
public class DeliveryBatch {

    @Id
    private String id;

    @Indexed
    private String staffId;

    /** Danh sách Delivery IDs trong chuyến này */
    private List<String> deliveryIds;

    /** ACTIVE: đang giao, COMPLETED: xong hết, CANCELLED */
    private String status;

    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
}
