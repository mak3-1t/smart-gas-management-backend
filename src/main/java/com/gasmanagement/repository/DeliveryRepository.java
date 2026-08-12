package com.gasmanagement.repository;

import com.gasmanagement.model.Delivery;
import com.gasmanagement.model.enums.DeliveryStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface DeliveryRepository extends MongoRepository<Delivery, String> {
    List<Delivery> findByStaffId(String staffId);
    List<Delivery> findByDeliveryStatus(DeliveryStatus status);
    List<Delivery> findByBatchId(String batchId);

    /** Tìm đơn đang chờ phân công */
    List<Delivery> findByDeliveryStatusOrderByCreatedAtAsc(DeliveryStatus status);

    /** Tìm tất cả đơn active của 1 Staff (Batch Delivery) */
    List<Delivery> findByStaffIdAndDeliveryStatusIn(String staffId, List<DeliveryStatus> statuses);
}
