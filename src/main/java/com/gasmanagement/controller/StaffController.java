package com.gasmanagement.controller;

import com.gasmanagement.dto.request.StaffStatusUpdateRequest;
import com.gasmanagement.dto.response.ApiResponse;
import com.gasmanagement.dto.response.DeliveryResponse;
import com.gasmanagement.dto.response.StaffProfileResponse;
import com.gasmanagement.service.StaffService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller cho Staff Management.
 *
 * <p>Base path: {@code /api/staff}
 *
 * <ul>
 *   <li>MANAGER: quản lý danh sách, tạo profile, cập nhật vehicle</li>
 *   <li>STAFF: tự update trạng thái, xem lịch sử delivery của bản thân</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/staff")
@RequiredArgsConstructor
public class StaffController {

    private final StaffService staffService;

    // ─────────────── MANAGER ENDPOINTS ───────────────

    /**
     * Tạo StaffProfile cho user có role STAFF.
     * Gọi ngay sau khi Manager tạo tài khoản Staff mới.
     *
     * <pre>POST /api/staff/{userId}/profile</pre>
     */
    @PostMapping("/{userId}/profile")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<StaffProfileResponse>> createStaffProfile(
            @PathVariable String userId) {

        StaffProfileResponse profile = staffService.createStaffProfile(userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Tạo staff profile thành công", profile));
    }

    /**
     * Lấy danh sách tất cả Staff (Manager view).
     *
     * <pre>GET /api/staff</pre>
     */
    @GetMapping
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<List<StaffProfileResponse>>> getAllStaff() {
        return ResponseEntity.ok(ApiResponse.ok(staffService.getAllStaff()));
    }

    /**
     * Lấy danh sách Staff đang AVAILABLE – dùng khi Manager cần assign đơn.
     *
     * <pre>GET /api/staff/available</pre>
     */
    @GetMapping("/available")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<List<StaffProfileResponse>>> getAvailableStaff() {
        return ResponseEntity.ok(ApiResponse.ok(staffService.getAvailableStaff()));
    }

    /**
     * Lấy thông tin chi tiết 1 Staff theo userId.
     *
     * <pre>GET /api/staff/{userId}</pre>
     */
    @GetMapping("/{userId}")
    @PreAuthorize("hasAnyRole('MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<StaffProfileResponse>> getStaffByUserId(
            @PathVariable String userId) {

        return ResponseEntity.ok(ApiResponse.ok(staffService.getStaffByUserId(userId)));
    }

    /**
     * Manager cập nhật thông tin phương tiện của Staff.
     *
     * <pre>PUT /api/staff/{userId}/vehicle</pre>
     *
     * @param vehicleInfo thông tin phương tiện (query param hoặc body đơn giản)
     */
    @PutMapping("/{userId}/vehicle")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<StaffProfileResponse>> updateVehicleInfo(
            @PathVariable String userId,
            @RequestParam String vehicleInfo) {

        return ResponseEntity.ok(
                ApiResponse.ok("Cập nhật thông tin phương tiện thành công",
                        staffService.updateVehicleInfo(userId, vehicleInfo)));
    }

    // ─────────────── STAFF SELF-SERVICE ENDPOINTS ───────────────

    /**
     * Staff tự cập nhật trạng thái làm việc: AVAILABLE hoặc OFFLINE.
     * Staff không được tự set BUSY (hệ thống tự set khi nhận đơn).
     *
     * <pre>PUT /api/staff/me/status</pre>
     *
     * @param userId lấy từ path param (sẽ được extract từ JWT sau)
     */
    @PutMapping("/{userId}/status")
    @PreAuthorize("hasAnyRole('STAFF', 'MANAGER')")
    public ResponseEntity<ApiResponse<StaffProfileResponse>> updateStaffStatus(
            @PathVariable String userId,
            @Valid @RequestBody StaffStatusUpdateRequest request) {

        return ResponseEntity.ok(
                ApiResponse.ok("Cập nhật trạng thái thành công",
                        staffService.updateStaffStatus(userId, request)));
    }

    /**
     * Staff xem lịch sử giao hàng của bản thân.
     *
     * <pre>GET /api/staff/{userId}/deliveries</pre>
     */
    @GetMapping("/{userId}/deliveries")
    @PreAuthorize("hasAnyRole('STAFF', 'MANAGER')")
    public ResponseEntity<ApiResponse<List<DeliveryResponse>>> getDeliveryHistory(
            @PathVariable String userId) {

        return ResponseEntity.ok(ApiResponse.ok(staffService.getDeliveryHistory(userId)));
    }
}
