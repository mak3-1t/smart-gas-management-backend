package com.gasmanagement.controller;

import com.gasmanagement.dto.request.OrderApprovalRequest;
import com.gasmanagement.dto.response.ApiResponse;
import com.gasmanagement.dto.response.OrderResponse;
import com.gasmanagement.service.OrderApprovalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller cho Manager Order Approval.
 * Base URL: /api/v1/manager/orders
 *
 * Tất cả endpoints yêu cầu role MANAGER hoặc SYSTEM_ADMIN.
 */
@RestController
@RequestMapping("/api/v1/manager/orders")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('MANAGER', 'SYSTEM_ADMIN')")
public class OrderApprovalController {

    private final OrderApprovalService orderApprovalService;

    // ─────────────────────────────────────────────────────
    //  READ – Xem danh sách Order
    // ─────────────────────────────────────────────────────

    /**
     * [MANAGER] Danh sách đơn đang chờ duyệt (PENDING_APPROVAL)
     * GET /api/v1/manager/orders/pending
     *
     * Sắp xếp ASC theo createdAt - đơn cũ nhất duyệt trước.
     */
    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getPendingOrders() {
        return ResponseEntity.ok(
                ApiResponse.success(orderApprovalService.getPendingOrders())
        );
    }

    /**
     * [MANAGER] Toàn bộ danh sách đơn hàng
     * GET /api/v1/manager/orders
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getAllOrders() {
        return ResponseEntity.ok(
                ApiResponse.success(orderApprovalService.getAllOrders())
        );
    }

    /**
     * [MANAGER] Chi tiết 1 đơn hàng
     * GET /api/v1/manager/orders/{orderId}
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderById(
            @PathVariable String orderId) {
        return ResponseEntity.ok(
                ApiResponse.success(orderApprovalService.getOrderById(orderId))
        );
    }

    /**
     * [MANAGER] Filter đơn hàng theo ApprovalStatus
     * GET /api/v1/manager/orders/filter?approvalStatus=PENDING_APPROVAL
     *
     * approvalStatus: PENDING_APPROVAL | APPROVED | REJECTED
     */
    @GetMapping("/filter")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getByApprovalStatus(
            @RequestParam String approvalStatus) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        orderApprovalService.getOrdersByApprovalStatus(approvalStatus))
        );
    }

    // ─────────────────────────────────────────────────────
    //  APPROVAL ACTIONS
    // ─────────────────────────────────────────────────────

    /**
     * [MANAGER] Duyệt đơn hàng
     * POST /api/v1/manager/orders/{orderId}/approve
     *
     * Body: { "managerId": "...", "note": "..." }
     *
     * Business logic:
     *  1. Validate order PENDING_APPROVAL
     *  2. Kiểm tra tồn kho đủ
     *  3. ApprovalStatus → APPROVED
     *  4. Reserve stock
     */
    @PostMapping("/{orderId}/approve")
    public ResponseEntity<ApiResponse<OrderResponse>> approveOrder(
            @PathVariable String orderId,
            @Valid @RequestBody OrderApprovalRequest request) {
        OrderResponse response = orderApprovalService.approveOrder(orderId, request);
        return ResponseEntity.ok(
                ApiResponse.success("Đơn hàng đã được duyệt thành công", response)
        );
    }

    /**
     * [MANAGER] Từ chối đơn hàng
     * POST /api/v1/manager/orders/{orderId}/reject
     *
     * Body: { "managerId": "...", "note": "Lý do từ chối bắt buộc" }
     *
     * Business logic:
     *  1. Validate order PENDING_APPROVAL
     *  2. ApprovalStatus → REJECTED
     *  3. OrderStatus → CANCELLED
     *  4. Nếu đã PAID → PaymentStatus → REFUND_PENDING
     */
    @PostMapping("/{orderId}/reject")
    public ResponseEntity<ApiResponse<OrderResponse>> rejectOrder(
            @PathVariable String orderId,
            @Valid @RequestBody OrderApprovalRequest request) {
        OrderResponse response = orderApprovalService.rejectOrder(orderId, request);
        return ResponseEntity.ok(
                ApiResponse.success("Đơn hàng đã bị từ chối", response)
        );
    }
}
