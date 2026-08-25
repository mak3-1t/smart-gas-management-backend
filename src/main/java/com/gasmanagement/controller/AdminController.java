package com.gasmanagement.controller;

import com.gasmanagement.dto.request.AccountStatusUpdateRequest;
import com.gasmanagement.dto.request.RoleUpdateRequest;
import com.gasmanagement.dto.response.ApiResponse;
import com.gasmanagement.dto.response.admin.AuditLogResponse;
import com.gasmanagement.model.User;
import com.gasmanagement.security.UserDetailsImpl;
import com.gasmanagement.service.AdminService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Controller cho System Admin.
 * 
 * <p>Base path: {@code /api/admin}
 * <p>Yêu cầu Role: {@code SYSTEM_ADMIN}
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SYSTEM_ADMIN')")
public class AdminController {

    private final AdminService adminService;

    // ─────────────── ACCOUNT MANAGEMENT ───────────────

    /**
     * Lấy danh sách tất cả Accounts.
     */
    @GetMapping("/accounts")
    public ResponseEntity<ApiResponse<Page<User>>> getAllAccounts(
            @PageableDefault(size = 20) Pageable pageable) {
        
        return ResponseEntity.ok(ApiResponse.ok(adminService.getAllAccounts(pageable)));
    }

    /**
     * Enable / Disable / Lock Account.
     */
    @PutMapping("/accounts/{userId}/status")
    public ResponseEntity<ApiResponse<User>> updateAccountStatus(
            @PathVariable String userId,
            @Valid @RequestBody AccountStatusUpdateRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            HttpServletRequest httpRequest) {
        
        User updatedUser = adminService.updateAccountStatus(userId, request, userDetails.getId(), httpRequest);
        return ResponseEntity.ok(ApiResponse.ok(updatedUser));
    }

    /**
     * Change Account Role.
     */
    @PutMapping("/accounts/{userId}/role")
    public ResponseEntity<ApiResponse<User>> updateAccountRole(
            @PathVariable String userId,
            @Valid @RequestBody RoleUpdateRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            HttpServletRequest httpRequest) {
        
        User updatedUser = adminService.updateAccountRole(userId, request, userDetails.getId(), httpRequest);
        return ResponseEntity.ok(ApiResponse.ok(updatedUser));
    }

    // ─────────────── AUDIT LOGS ───────────────

    /**
     * Xem Audit Logs chung.
     */
    @GetMapping("/audit-logs")
    public ResponseEntity<ApiResponse<Page<AuditLogResponse>>> getAuditLogs(
            @PageableDefault(size = 20, sort = "createdAt", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String entityId) {
        
        Page<AuditLogResponse> logs;
        if (action != null) {
            logs = adminService.getAuditLogsByAction(action, pageable);
        } else if (entityType != null && entityId != null) {
            logs = adminService.getAuditLogsByEntity(entityType, entityId, pageable);
        } else {
            logs = adminService.getAuditLogs(pageable);
        }
        
        return ResponseEntity.ok(ApiResponse.ok(logs));
    }
}
