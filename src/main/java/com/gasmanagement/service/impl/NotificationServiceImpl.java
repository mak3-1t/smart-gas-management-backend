package com.gasmanagement.service.impl;

import com.gasmanagement.exception.ResourceNotFoundException;
import com.gasmanagement.model.Notification;
import com.gasmanagement.model.enums.UserRole;
import com.gasmanagement.repository.NotificationRepository;
import com.gasmanagement.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;

    @Override
    public Page<Notification> getUserNotifications(String userId, Pageable pageable) {
        return notificationRepository.findByReceiverIdOrderByCreatedAtDesc(userId, pageable);
    }

    @Override
    public long getUnreadCount(String userId) {
        return notificationRepository.countByReceiverIdAndIsReadFalse(userId);
    }

    @Override
    public void markAsRead(String notificationId, String userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông báo: " + notificationId));
        if (notification.getReceiverId() != null && notification.getReceiverId().equals(userId)) {
            notification.setRead(true);
            notificationRepository.save(notification);
        }
    }

    @Override
    public void markAllAsRead(String userId) {
        List<Notification> unreadList = notificationRepository.findByReceiverIdAndIsReadFalseOrderByCreatedAtDesc(userId);
        if (!unreadList.isEmpty()) {
            unreadList.forEach(n -> n.setRead(true));
            notificationRepository.saveAll(unreadList);
        }
    }

    @Override
    public Notification sendNotification(String receiverId, UserRole role, String title, String body, String type, String relatedId) {
        Notification notification = Notification.builder()
                .receiverId(receiverId)
                .targetRole(role)
                .title(title)
                .body(body)
                .type(type)
                .relatedId(relatedId)
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();
        Notification saved = notificationRepository.save(notification);
        log.info("Sent notification {} to user {}: {}", saved.getId(), receiverId, title);
        return saved;
    }
}
