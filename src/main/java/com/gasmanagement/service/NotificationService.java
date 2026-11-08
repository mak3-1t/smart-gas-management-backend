package com.gasmanagement.service;

import com.gasmanagement.model.Notification;
import com.gasmanagement.model.enums.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface NotificationService {
    Page<Notification> getUserNotifications(String userId, Pageable pageable);
    long getUnreadCount(String userId);
    void markAsRead(String notificationId, String userId);
    void markAllAsRead(String userId);
    Notification sendNotification(String receiverId, UserRole role, String title, String body, String type, String relatedId);
}
