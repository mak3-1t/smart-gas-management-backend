package com.gasmanagement.repository;

import com.gasmanagement.model.Payment;
import com.gasmanagement.model.enums.PaymentStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends MongoRepository<Payment, String> {
    Optional<Payment> findByOrderId(String orderId);
    List<Payment> findByCustomerId(String customerId);
    List<Payment> findByPaymentStatus(PaymentStatus status);
}
