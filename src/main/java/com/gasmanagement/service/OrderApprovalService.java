package com.gasmanagement.service;

import com.gasmanagement.dto.request.OrderApprovalRequest;
import com.gasmanagement.dto.response.OrderResponse;

import java.util.List;

/**
 * Service quản lý luồng Manager Approve/Reject Order.
 *
 * DEV 2 owns: OrderApprovalService
 * Tích hợp với: InventoryService (reserve/release stock)
 */
public interface OrderApprovalService {

    // ─────────────── READ ───────────────

    /**
     * Danh sách Order đang chờ Manager duyệt (PENDING_APPROVAL).
     * Sắp xếp theo thời gian tạo ASC (đơn cũ nhất duyệt trước).
     */
    List<OrderResponse> getPendingOrders();

    /**
     * Lấy chi tiết 1 Order (Manager view).
     */
    OrderResponse getOrderById(String orderId);

    /**
     * Toàn bộ danh sách Order cho Manager (có thể filter).
     */
    List<OrderResponse> getAllOrders();

    /**
     * Danh sách Order theo trạng thái ApprovalStatus.
     */
    List<OrderResponse> getOrdersByApprovalStatus(String approvalStatus);

    // ─────────────── APPROVAL ACTIONS ───────────────

    /**
     * Manager APPROVE một Order.
     *
     * Business logic:
     * 1. Validate order tồn tại và đang PENDING_APPROVAL
     * 2. Kiểm tra tồn kho còn đủ cho từng item không
     * 3. Set ApprovalStatus = APPROVED
     * 4. Ghi ApprovalHistory
     * 5. Reserve stock cho từng item (InventoryService.reserveStock)
     */
    OrderResponse approveOrder(String orderId, OrderApprovalRequest request);

    /**
     * Manager REJECT một Order.
     *
     * Business logic:
     * 1. Validate order tồn tại và đang PENDING_APPROVAL
     * 2. Set ApprovalStatus = REJECTED
     * 3. Set OrderStatus = CANCELLED
     * 4. Ghi ApprovalHistory + StatusHistory
     * 5. Nếu đã PAID → set PaymentStatus = REFUND_PENDING
     * 6. Release stock nếu đã reserve (an toàn)
     */
    OrderResponse rejectOrder(String orderId, OrderApprovalRequest request);
}
