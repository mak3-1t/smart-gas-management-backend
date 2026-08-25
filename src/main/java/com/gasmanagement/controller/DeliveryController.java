package com.gasmanagement.controller;

import com.gasmanagement.dto.request.DeliveryActionRequest;
import com.gasmanagement.dto.request.DeliveryAssignRequest;
import com.gasmanagement.dto.response.ApiResponse;
import com.gasmanagement.dto.response.DeliveryResponse;
import com.gasmanagement.service.DeliveryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller cho toàn bộ luồng Delivery.
 *
 * <p>Base path: {@code /api/deliveries}
 *
 * <p>Flow chính:
 * <pre>
 * Manager assign → Staff accept/reject → Staff start → Staff complete/fail
 *                                        ↓
 *                              (Nếu EXCHANGE) cylinder-exchange
 * </pre>
 */
@RestController
@RequestMapping("/api/deliveries")
@RequiredArgsConstructor
public class DeliveryController {

    private final DeliveryService deliveryService;

    // ─────────────── MANAGER ENDPOINTS ───────────────

    /**
     * Manager phân công Staff giao đơn hàng.
     * Điều kiện: ApprovalStatus=APPROVED + Staff AVAILABLE.
     *
     * <pre>POST /api/deliveries/assign</pre>
     */
    @PostMapping("/assign")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<DeliveryResponse>> assignDelivery(
            @Valid @RequestBody DeliveryAssignRequest request) {

        DeliveryResponse response = deliveryService.assignDelivery(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Phân công giao hàng thành công", response));
    }

    /**
     * Lấy danh sách tất cả deliveries (Manager quản lý).
     *
     * <pre>GET /api/deliveries</pre>
     */
    @GetMapping
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<List<DeliveryResponse>>> getAllDeliveries() {
        return ResponseEntity.ok(ApiResponse.ok(deliveryService.getAllDeliveries()));
    }

    /**
     * Lấy danh sách delivery đang chờ phân công (WAITING).
     *
     * <pre>GET /api/deliveries/waiting</pre>
     */
    @GetMapping("/waiting")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<List<DeliveryResponse>>> getWaitingDeliveries() {
        return ResponseEntity.ok(ApiResponse.ok(deliveryService.getWaitingDeliveries()));
    }

    /**
     * Lấy chi tiết 1 delivery theo ID.
     *
     * <pre>GET /api/deliveries/{id}</pre>
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<DeliveryResponse>> getDeliveryById(
            @PathVariable String id) {

        return ResponseEntity.ok(ApiResponse.ok(deliveryService.getDeliveryById(id)));
    }

    /**
     * Lấy delivery theo orderId.
     *
     * <pre>GET /api/deliveries/order/{orderId}</pre>
     */
    @GetMapping("/order/{orderId}")
    @PreAuthorize("hasAnyRole('MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<DeliveryResponse>> getDeliveryByOrderId(
            @PathVariable String orderId) {

        return ResponseEntity.ok(ApiResponse.ok(deliveryService.getDeliveryByOrderId(orderId)));
    }

    /**
     * Lấy tất cả deliveries của 1 Staff (Manager xem performance).
     *
     * <pre>GET /api/deliveries/staff/{staffId}</pre>
     */
    @GetMapping("/staff/{staffId}")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<List<DeliveryResponse>>> getDeliveriesByStaff(
            @PathVariable String staffId) {

        return ResponseEntity.ok(ApiResponse.ok(deliveryService.getDeliveriesByStaff(staffId)));
    }

    // ─────────────── STAFF ACTION ENDPOINTS ───────────────

    /**
     * Staff chấp nhận đơn giao hàng.
     * DeliveryStatus: WAITING → ACCEPTED. StaffStatus → BUSY.
     *
     * <pre>POST /api/deliveries/{id}/accept</pre>
     */
    @PostMapping("/{id}/accept")
    @PreAuthorize("hasRole('STAFF')")
    public ResponseEntity<ApiResponse<DeliveryResponse>> acceptDelivery(
            @PathVariable String id,
            @RequestBody DeliveryActionRequest request) {

        return ResponseEntity.ok(
                ApiResponse.ok("Nhận đơn giao hàng thành công",
                        deliveryService.acceptDelivery(id, request)));
    }

    /**
     * Staff từ chối đơn giao hàng.
     * Staff vẫn AVAILABLE. Delivery về WAITING chờ assign lại.
     *
     * <pre>POST /api/deliveries/{id}/reject</pre>
     */
    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('STAFF')")
    public ResponseEntity<ApiResponse<DeliveryResponse>> rejectDelivery(
            @PathVariable String id,
            @RequestBody DeliveryActionRequest request) {

        return ResponseEntity.ok(
                ApiResponse.ok("Từ chối đơn giao hàng",
                        deliveryService.rejectDelivery(id, request)));
    }

    /**
     * Staff bắt đầu đi giao hàng.
     * DeliveryStatus: ACCEPTED → DELIVERING.
     *
     * <pre>POST /api/deliveries/{id}/start</pre>
     */
    @PostMapping("/{id}/start")
    @PreAuthorize("hasRole('STAFF')")
    public ResponseEntity<ApiResponse<DeliveryResponse>> startDelivery(
            @PathVariable String id,
            @RequestBody DeliveryActionRequest request) {

        return ResponseEntity.ok(
                ApiResponse.ok("Bắt đầu giao hàng",
                        deliveryService.startDelivery(id, request)));
    }

    /**
     * Staff xác nhận giao hàng thành công.
     * Kiểm tra: PaymentStatus=PAID, CylinderExchange=COMPLETED (nếu cần).
     * DeliveryStatus → DELIVERED. OrderStatus → DELIVERED.
     *
     * <pre>POST /api/deliveries/{id}/complete</pre>
     */
    @PostMapping("/{id}/complete")
    @PreAuthorize("hasRole('STAFF')")
    public ResponseEntity<ApiResponse<DeliveryResponse>> completeDelivery(
            @PathVariable String id,
            @RequestBody DeliveryActionRequest request) {

        return ResponseEntity.ok(
                ApiResponse.ok("Xác nhận giao hàng thành công",
                        deliveryService.completeDelivery(id, request)));
    }

    /**
     * Staff báo giao hàng thất bại.
     * DeliveryStatus → FAILED. Staff → AVAILABLE.
     * Manager sẽ quyết định reassign hoặc cancel.
     *
     * <pre>POST /api/deliveries/{id}/fail</pre>
     */
    @PostMapping("/{id}/fail")
    @PreAuthorize("hasRole('STAFF')")
    public ResponseEntity<ApiResponse<DeliveryResponse>> failDelivery(
            @PathVariable String id,
            @RequestBody DeliveryActionRequest request) {

        return ResponseEntity.ok(
                ApiResponse.ok("Đã ghi nhận giao hàng thất bại",
                        deliveryService.failDelivery(id, request)));
    }

    /**
     * Staff xử lý vỏ bình khi giao hàng kiểu EXCHANGE.
     * Xác nhận tình trạng vỏ bình thu hồi hoặc báo khách không có vỏ.
     *
     * <pre>POST /api/deliveries/{id}/cylinder-exchange</pre>
     */
    @PostMapping("/{id}/cylinder-exchange")
    @PreAuthorize("hasRole('STAFF')")
    public ResponseEntity<ApiResponse<DeliveryResponse>> processCylinderExchange(
            @PathVariable String id,
            @RequestBody DeliveryActionRequest request) {

        return ResponseEntity.ok(
                ApiResponse.ok("Xử lý vỏ bình thành công",
                        deliveryService.processCylinderExchange(id, request)));
    }
}
