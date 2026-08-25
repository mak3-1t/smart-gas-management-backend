package com.gasmanagement.service;

import com.gasmanagement.dto.request.AccountStatusUpdateRequest;
import com.gasmanagement.dto.request.RoleUpdateRequest;
import com.gasmanagement.dto.response.admin.AuditLogResponse;
import com.gasmanagement.model.User;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service cho System Admin quản lý hệ thống.
 */
public interface AdminService {

    /**
     * Lấy danh sách tất cả tài khoản (có phân trang).
     */
    Page<User> getAllAccounts(Pageable pageable);

    /**
     * Thay đổi trạng thái tài khoản (ACTIVE, LOCKED, DISABLED).
     * 
     * @param userId ID tài khoản cần thay đổi
     * @param request Data thay đổi trạng thái
     * @param adminId ID của admin đang thực hiện
     * @param httpRequest Request để lấy IP ghi log
     */
    User updateAccountStatus(String userId, AccountStatusUpdateRequest request, String adminId, HttpServletRequest httpRequest);

    /**
     * Thay đổi Role của tài khoản.
     * 
     * @param userId ID tài khoản cần đổi role
     * @param request Data đổi role
     * @param adminId ID của admin đang thực hiện
     * @param httpRequest Request để lấy IP ghi log
     */
    User updateAccountRole(String userId, RoleUpdateRequest request, String adminId, HttpServletRequest httpRequest);

    /**
     * Lấy danh sách Audit Logs (có phân trang).
     */
    Page<AuditLogResponse> getAuditLogs(Pageable pageable);
    
    /**
     * Tìm Audit Logs theo entityType và entityId.
     */
    Page<AuditLogResponse> getAuditLogsByEntity(String entityType, String entityId, Pageable pageable);
    
    /**
     * Tìm Audit Logs theo action.
     */
    Page<AuditLogResponse> getAuditLogsByAction(String action, Pageable pageable);
}
