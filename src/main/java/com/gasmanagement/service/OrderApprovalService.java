package com.gasmanagement.service;

public interface OrderApprovalService {
    
    /**
     * Manager duyệt đơn hàng.
     * Trạng thái ApprovalStatus sẽ chuyển sang APPROVED.
     * System sẽ tự động tìm Staff và phân công đơn hàng (DeliveryAssignment).
     */
    void approveOrder(String orderId, String managerId, String note);
    
    /**
     * Manager từ chối đơn hàng.
     * Trạng thái ApprovalStatus sẽ chuyển sang REJECTED.
     * OrderStatus sẽ chuyển sang CANCELLED.
     */
    void rejectOrder(String orderId, String managerId, String note);
}
