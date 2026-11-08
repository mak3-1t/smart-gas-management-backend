package com.gasmanagement.repository;

import com.gasmanagement.model.DeliveryBatch;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DeliveryBatchRepository extends MongoRepository<DeliveryBatch, String> {
    List<DeliveryBatch> findByStaffId(String staffId);
    List<DeliveryBatch> findByStatus(String status);
}
