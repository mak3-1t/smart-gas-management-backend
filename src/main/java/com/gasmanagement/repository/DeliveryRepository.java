package com.gasmanagement.repository;

import com.gasmanagement.model.Delivery;
import com.gasmanagement.model.enums.DeliveryStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface DeliveryRepository extends MongoRepository<Delivery, String> {
    List<Delivery> findByStaffId(String staffId);
    List<Delivery> findByDeliveryStatus(DeliveryStatus status);
    List<Delivery> findByBatchId(String batchId);

    /** Tìm Delivery theo orderId – mỗi order chỉ có 1 delivery active */
    Optional<Delivery> findByOrderId(String orderId);

    /** Tìm tất cả Delivery theo orderId (lịch sử reassign) */
    List<Delivery> findAllByOrderId(String orderId);

    /** Tìm đơn đang chờ phân công */
    List<Delivery> findByDeliveryStatusOrderByCreatedAtAsc(DeliveryStatus status);

    /** Tìm tất cả đơn active của 1 Staff (Batch Delivery) */
    List<Delivery> findByStaffIdAndDeliveryStatusIn(String staffId, List<DeliveryStatus> statuses);
}
