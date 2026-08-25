package com.gasmanagement.service.impl;

import com.gasmanagement.dto.request.AccountStatusUpdateRequest;
import com.gasmanagement.dto.request.RoleUpdateRequest;
import com.gasmanagement.dto.response.admin.AuditLogResponse;
import com.gasmanagement.exception.BusinessException;
import com.gasmanagement.exception.ResourceNotFoundException;
import com.gasmanagement.model.AuditLog;
import com.gasmanagement.model.User;
import com.gasmanagement.model.enums.AccountStatus;
import com.gasmanagement.model.enums.UserRole;
import com.gasmanagement.repository.AuditLogRepository;
import com.gasmanagement.repository.UserRepository;
import com.gasmanagement.service.AdminService;
import com.gasmanagement.service.AuditService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;
    private final AuditService auditService;

    @Override
    public Page<User> getAllAccounts(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    @Override
    public User updateAccountStatus(String userId, AccountStatusUpdateRequest request, String adminId, HttpServletRequest httpRequest) {
        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        // Kiểm tra không cho admin tự block chính mình
        if (userId.equals(adminId)) {
            throw new BusinessException("You cannot change your own account status");
        }

        // Parse new status
        AccountStatus newStatus;
        try {
            newStatus = AccountStatus.valueOf(request.getStatus().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Invalid account status. Allowed values: ACTIVE, LOCKED, DISABLED");
        }

        AccountStatus oldStatus = targetUser.getStatus();

        if (oldStatus == newStatus) {
            return targetUser; // Không có gì thay đổi
        }

        // Nếu SYSTEM_ADMIN cuối cùng đang active thì không cho disable/lock
        if (targetUser.getRole() == UserRole.SYSTEM_ADMIN && newStatus != AccountStatus.ACTIVE) {
            long activeAdmins = userRepository.findAll().stream()
                    .filter(u -> u.getRole() == UserRole.SYSTEM_ADMIN && u.getStatus() == AccountStatus.ACTIVE)
                    .count();
            if (activeAdmins <= 1) {
                throw new BusinessException("Cannot disable/lock the last active SYSTEM_ADMIN");
            }
        }

        // Cập nhật status
        targetUser.setStatus(newStatus);
        targetUser.setUpdatedAt(LocalDateTime.now());
        User savedUser = userRepository.save(targetUser);

        // Ghi Audit Log
        String actionName = "UPDATE_ACCOUNT_STATUS";
        String auditOldValue = oldStatus.name();
        String auditNewValue = newStatus.name() + (request.getReason() != null ? " (Reason: " + request.getReason() + ")" : "");
        
        auditService.logAction(adminId, UserRole.SYSTEM_ADMIN, actionName, "User", userId, auditOldValue, auditNewValue, httpRequest);
        
        log.info("Admin {} changed status of user {} from {} to {}", adminId, userId, oldStatus, newStatus);
        return savedUser;
    }

    @Override
    public User updateAccountRole(String userId, RoleUpdateRequest request, String adminId, HttpServletRequest httpRequest) {
        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        // Kiểm tra không cho admin tự đổi role chính mình
        if (userId.equals(adminId)) {
            throw new BusinessException("You cannot change your own role");
        }

        // Parse new role
        UserRole newRole;
        try {
            newRole = UserRole.valueOf(request.getRole().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Invalid role. Allowed values: CUSTOMER, STAFF, MANAGER, SYSTEM_ADMIN");
        }

        UserRole oldRole = targetUser.getRole();

        if (oldRole == newRole) {
            return targetUser; // Không có gì thay đổi
        }

        // Cập nhật role
        targetUser.setRole(newRole);
        targetUser.setUpdatedAt(LocalDateTime.now());
        User savedUser = userRepository.save(targetUser);

        // Ghi Audit Log
        String actionName = "UPDATE_ACCOUNT_ROLE";
        String auditOldValue = oldRole.name();
        String auditNewValue = newRole.name() + (request.getReason() != null ? " (Reason: " + request.getReason() + ")" : "");
        
        auditService.logAction(adminId, UserRole.SYSTEM_ADMIN, actionName, "User", userId, auditOldValue, auditNewValue, httpRequest);
        
        log.info("Admin {} changed role of user {} from {} to {}", adminId, userId, oldRole, newRole);
        return savedUser;
    }

    @Override
    public Page<AuditLogResponse> getAuditLogs(Pageable pageable) {
        return auditLogRepository.findAll(pageable).map(this::mapToAuditLogResponse);
    }

    @Override
    public Page<AuditLogResponse> getAuditLogsByEntity(String entityType, String entityId, Pageable pageable) {
        return auditLogRepository.findByEntityTypeAndEntityId(entityType, entityId, pageable)
                .map(this::mapToAuditLogResponse);
    }

    @Override
    public Page<AuditLogResponse> getAuditLogsByAction(String action, Pageable pageable) {
        return auditLogRepository.findByAction(action, pageable).map(this::mapToAuditLogResponse);
    }

    private AuditLogResponse mapToAuditLogResponse(AuditLog auditLog) {
        String username = "Unknown";
        if (auditLog.getActorId() != null) {
            username = userRepository.findById(auditLog.getActorId())
                    .map(User::getUsername)
                    .orElse("Unknown");
        }

        return AuditLogResponse.builder()
                .id(auditLog.getId())
                .actorId(auditLog.getActorId())
                .actorUsername(username)
                .actorRole(auditLog.getActorRole())
                .action(auditLog.getAction())
                .entityType(auditLog.getEntityType())
                .entityId(auditLog.getEntityId())
                .oldValue(auditLog.getOldValue())
                .newValue(auditLog.getNewValue())
                .ipAddress(auditLog.getIpAddress())
                .createdAt(auditLog.getCreatedAt())
                .build();
    }
}
