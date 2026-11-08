package com.gasmanagement.model;

import com.gasmanagement.model.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "notifications")
public class Notification {
    @Id
    private String id;
    private String receiverId;
    private UserRole targetRole;
    private String title;
    private String body;
    private String type; // ORDER_CREATED, ORDER_APPROVED, DELIVERY_ASSIGNED, etc.
    private String relatedId;
    private boolean isRead;
    private LocalDateTime createdAt;
}
