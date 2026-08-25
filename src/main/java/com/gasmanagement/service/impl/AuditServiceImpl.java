package com.gasmanagement.service.impl;

import com.gasmanagement.model.AuditLog;
import com.gasmanagement.model.enums.UserRole;
import com.gasmanagement.repository.AuditLogRepository;
import com.gasmanagement.service.AuditService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditServiceImpl implements AuditService {

    private final AuditLogRepository auditLogRepository;

    @Async
    @Override
    public void logAction(String actorId, UserRole actorRole, String action,
                          String entityType, String entityId,
                          Object oldValue, Object newValue,
                          HttpServletRequest request) {
        
        String ipAddress = null;
        if (request != null) {
            ipAddress = getClientIpAddress(request);
        }

        AuditLog auditLog = AuditLog.builder()
                .actorId(actorId)
                .actorRole(actorRole != null ? actorRole.name() : null)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .oldValue(oldValue)
                .newValue(newValue)
                .ipAddress(ipAddress)
                .createdAt(LocalDateTime.now())
                .build();

        try {
            auditLogRepository.save(auditLog);
            log.info("Audit log saved: Actor {} did {} on {} {}", actorId, action, entityType, entityId);
        } catch (Exception e) {
            log.error("Failed to save audit log: {}", e.getMessage(), e);
        }
    }

    @Async
    @Override
    public void logAction(String actorId, UserRole actorRole, String action,
                          String entityType, String entityId,
                          Object oldValue, Object newValue) {
        logAction(actorId, actorRole, action, entityType, entityId, oldValue, newValue, null);
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String[] headerNames = {
            "X-Forwarded-For",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_CLIENT_IP",
            "HTTP_X_FORWARDED_FOR"
        };
        
        for (String header : headerNames) {
            String ip = request.getHeader(header);
            if (ip != null && ip.length() != 0 && !"unknown".equalsIgnoreCase(ip)) {
                // X-Forwarded-For có thể chứa danh sách IPs, IP đầu tiên là IP thật của client
                return ip.split(",")[0].trim();
            }
        }
        return request.getRemoteAddr();
    }
}
