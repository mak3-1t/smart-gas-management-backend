package com.gasmanagement.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "audit_logs")
public class AuditLog {

    @Id
    private String id;

    @Indexed
    private String actorId;

    private String actorRole;
    private String action;

    @Indexed
    private String entityType;

    @Indexed
    private String entityId;

    private Object oldValue;
    private Object newValue;
    private String ipAddress;

    @Indexed
    private LocalDateTime createdAt;
}
