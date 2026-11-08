package com.gasmanagement.repository;

import com.gasmanagement.model.Refund;
import com.gasmanagement.model.enums.RefundStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RefundRepository extends MongoRepository<Refund, String> {
    Optional<Refund> findByOrderId(String orderId);
    List<Refund> findByCustomerId(String customerId);
    List<Refund> findByStatus(RefundStatus status);
}
