package com.gasmanagement.dto.response.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO cho Audit Log.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogResponse {
    private String id;
    private String actorId;
    private String actorUsername;
    private String actorRole;
    
    private String action;
    private String entityType;
    private String entityId;
    
    private Object oldValue;
    private Object newValue;
    private String ipAddress;
    
    private LocalDateTime createdAt;
}
