package com.gasmanagement.controller;

import com.gasmanagement.dto.request.AccountStatusUpdateRequest;
import com.gasmanagement.dto.request.RegisterRequest;
import com.gasmanagement.dto.request.RoleUpdateRequest;
import com.gasmanagement.dto.response.ApiResponse;
import com.gasmanagement.dto.response.CustomerSummaryResponse;
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

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/accounts")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<Page<User>>> getAllAccounts(
            @PageableDefault(size = 50) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(adminService.getAllAccounts(pageable)));
    }

    @GetMapping("/customers")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<List<CustomerSummaryResponse>>> getCustomers(
            @RequestParam(required = false) String keyword) {
        return ResponseEntity.ok(ApiResponse.ok(adminService.getCustomerSummaries(keyword)));
    }

    @PostMapping("/reset-transactional-data")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<java.util.Map<String, Object>>> resetTransactionalData() {
        return ResponseEntity.ok(ApiResponse.ok(
                "Đã xóa sạch dữ liệu giao dịch thử nghiệm về trạng thái gốc",
                adminService.resetTransactionalData()));
    }

    @PostMapping("/accounts")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<User>> createAccount(
            @Valid @RequestBody RegisterRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            HttpServletRequest httpRequest) {
        User created = adminService.createAccount(request, userDetails.getId(), httpRequest);
        return ResponseEntity.ok(ApiResponse.ok("Tạo tài khoản thành công", created));
    }

    @GetMapping("/managers")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<List<CustomerSummaryResponse>>> getManagementAccounts() {
        return ResponseEntity.ok(ApiResponse.ok(adminService.getManagementAccounts()));
    }

    @PutMapping("/accounts/{userId}/password")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<User>> resetUserPassword(
            @PathVariable String userId,
            @RequestBody java.util.Map<String, String> body,
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            HttpServletRequest httpRequest) {
        String newPassword = body != null ? body.get("newPassword") : null;
        User updated = adminService.resetUserPassword(userId, newPassword, userDetails.getId(), httpRequest);
        return ResponseEntity.ok(ApiResponse.ok("Đổi mật khẩu thành công", updated));
    }

    @DeleteMapping("/accounts/{userId}")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<Void>> deleteAccount(
            @PathVariable String userId,
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            HttpServletRequest httpRequest) {
        adminService.deleteAccount(userId, userDetails.getId(), httpRequest);
        return ResponseEntity.ok(ApiResponse.ok("Đã xóa tài khoản thành công", null));
    }

    @PutMapping("/accounts/{userId}/status")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<User>> updateAccountStatus(
            @PathVariable String userId,
            @Valid @RequestBody AccountStatusUpdateRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            HttpServletRequest httpRequest) {
        User updatedUser = adminService.updateAccountStatus(userId, request, userDetails.getId(), httpRequest);
        return ResponseEntity.ok(ApiResponse.ok(updatedUser));
    }

    @PutMapping("/accounts/{userId}/role")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<User>> updateAccountRole(
            @PathVariable String userId,
            @Valid @RequestBody RoleUpdateRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            HttpServletRequest httpRequest) {
        User updatedUser = adminService.updateAccountRole(userId, request, userDetails.getId(), httpRequest);
        return ResponseEntity.ok(ApiResponse.ok(updatedUser));
    }

    @GetMapping("/audit-logs")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
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
