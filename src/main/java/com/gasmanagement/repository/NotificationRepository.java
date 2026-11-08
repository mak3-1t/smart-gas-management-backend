package com.gasmanagement.repository;

import com.gasmanagement.model.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends MongoRepository<Notification, String> {
    Page<Notification> findByReceiverIdOrderByCreatedAtDesc(String receiverId, Pageable pageable);
    List<Notification> findByReceiverIdAndIsReadFalseOrderByCreatedAtDesc(String receiverId);
    long countByReceiverIdAndIsReadFalse(String receiverId);
}
