package com.gasmanagement.service;

import com.gasmanagement.dto.request.AccountStatusUpdateRequest;
import com.gasmanagement.dto.request.RegisterRequest;
import com.gasmanagement.dto.request.RoleUpdateRequest;
import com.gasmanagement.dto.response.CustomerSummaryResponse;
import com.gasmanagement.dto.response.admin.AuditLogResponse;
import com.gasmanagement.model.User;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Service cho System Admin quản lý hệ thống.
 */
public interface AdminService {

    /**
     * Lấy danh sách tất cả tài khoản (có phân trang).
     */
    Page<User> getAllAccounts(Pageable pageable);

    /**
     * Lấy danh sách thống kê khách hàng (CUSTOMER) kèm tổng đơn hàng, tổng chi
     * tiêu, địa chỉ.
     */
    List<CustomerSummaryResponse> getCustomerSummaries(String keyword);

    /**
     * Tạo tài khoản mới bởi Admin/Manager (CUSTOMER, STAFF, MANAGER, SYSTEM_ADMIN).
     */
    User createAccount(RegisterRequest request, String adminId, HttpServletRequest httpRequest);

    /**
     * Lấy danh sách tài khoản Quản lý (MANAGER), Quản trị (SYSTEM_ADMIN) và Nhân sự
     * (STAFF) kèm Họ tên.
     */
    List<CustomerSummaryResponse> getManagementAccounts();

    /**
     * Đổi mật khẩu cho khách hàng hoặc tài khoản nhân sự (khi quên mật khẩu).
     */
    User resetUserPassword(String userId, String newPassword, String actorId, HttpServletRequest httpRequest);

    /**
     * Xóa tài khoản khách hàng hoặc nhân sự.
     */
    void deleteAccount(String userId, String actorId, HttpServletRequest httpRequest);

    /**
     * Dọn dẹp toàn bộ dữ liệu giao dịch thử nghiệm (orders, deliveries, payments,
     * carts)
     * và reset thống kê giao hàng trên staff_profiles về 0.
     */
    java.util.Map<String, Object> resetTransactionalData();

    /**
     * Thay đổi trạng thái tài khoản (ACTIVE, LOCKED, DISABLED).
     * 
     * @param userId      ID tài khoản cần thay đổi
     * @param request     Data thay đổi trạng thái
     * @param adminId     ID của admin đang thực hiện
     * @param httpRequest Request để lấy IP ghi log
     */
    User updateAccountStatus(String userId, AccountStatusUpdateRequest request, String adminId,
            HttpServletRequest httpRequest);

    /**
     * Thay đổi Role của tài khoản.
     * 
     * @param userId      ID tài khoản cần đổi role
     * @param request     Data đổi role
     * @param adminId     ID của admin đang thực hiện
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
