package com.gasmanagement.repository;

import com.gasmanagement.model.Order;
import com.gasmanagement.model.enums.ApprovalStatus;
import com.gasmanagement.model.enums.OrderStatus;
import com.gasmanagement.model.enums.PaymentStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends MongoRepository<Order, String> {
    Optional<Order> findByOrderCode(String orderCode);
    List<Order> findByCustomerIdOrderByCreatedAtDesc(String customerId);
    List<Order> findByOrderStatus(OrderStatus status);
    List<Order> findByApprovalStatus(ApprovalStatus status);
    List<Order> findByPaymentStatus(PaymentStatus status);
    List<Order> findByApprovalStatusOrderByCreatedAtAsc(ApprovalStatus status);
    long countByOrderStatus(OrderStatus status);
    long countByApprovalStatus(ApprovalStatus status);
    long countByPaymentStatus(PaymentStatus status);
}
