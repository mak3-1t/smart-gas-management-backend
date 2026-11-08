package com.gasmanagement.repository;

import com.gasmanagement.model.Review;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends MongoRepository<Review, String> {
    List<Review> findByProductId(String productId);
    List<Review> findByCustomerId(String customerId);
    Optional<Review> findByOrderId(String orderId);
    boolean existsByOrderIdAndCustomerId(String orderId, String customerId);
}
