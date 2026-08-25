package com.gasmanagement.repository;

import com.gasmanagement.model.InventoryTransaction;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface InventoryTransactionRepository extends MongoRepository<InventoryTransaction, String> {
    List<InventoryTransaction> findByProductIdOrderByCreatedAtDesc(String productId);
    List<InventoryTransaction> findByOrderIdOrderByCreatedAtDesc(String orderId);
}
