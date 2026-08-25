package com.gasmanagement.service;

import com.gasmanagement.model.enums.UserRole;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Service để ghi log các thao tác quan trọng (Audit Logging).
 * Được sử dụng bởi các service khác khi thực hiện hành động.
 */
public interface AuditService {

    /**
     * Ghi lại một Audit Log.
     *
     * @param actorId     ID của người thực hiện hành động
     * @param actorRole   Role của người thực hiện (CUSTOMER, STAFF, MANAGER, SYSTEM_ADMIN)
     * @param action      Hành động (ví dụ: "UPDATE_PRODUCT_PRICE", "CONFIRM_COD")
     * @param entityType  Loại entity bị tác động (ví dụ: "Product", "Order")
     * @param entityId    ID của entity bị tác động
     * @param oldValue    Giá trị trước khi thay đổi (có thể null nếu là tạo mới)
     * @param newValue    Giá trị sau khi thay đổi
     * @param request     HttpServletRequest để lấy IP address (nếu có, có thể null)
     */
    void logAction(String actorId, UserRole actorRole, String action, 
                   String entityType, String entityId, 
                   Object oldValue, Object newValue, 
                   HttpServletRequest request);
                   
    /**
     * Helper method ghi log mà không cần truyền HttpServletRequest.
     */
    void logAction(String actorId, UserRole actorRole, String action, 
                   String entityType, String entityId, 
                   Object oldValue, Object newValue);
}
