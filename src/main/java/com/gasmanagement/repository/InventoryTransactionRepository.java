package com.gasmanagement.repository;

import com.gasmanagement.model.InventoryTransaction;
import com.gasmanagement.model.enums.InventoryTransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface InventoryTransactionRepository extends MongoRepository<InventoryTransaction, String> {
    Page<InventoryTransaction> findByProductIdOrderByCreatedAtDesc(String productId, Pageable pageable);
    List<InventoryTransaction> findByOrderId(String orderId);
    List<InventoryTransaction> findByProductIdAndCreatedAtBetween(String productId, LocalDateTime from, LocalDateTime to);
    List<InventoryTransaction> findByType(InventoryTransactionType type);
}
