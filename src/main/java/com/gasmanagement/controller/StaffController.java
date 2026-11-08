package com.gasmanagement.controller;

import com.gasmanagement.dto.request.StaffStatusUpdateRequest;
import com.gasmanagement.dto.response.ApiResponse;
import com.gasmanagement.dto.response.DeliveryResponse;
import com.gasmanagement.dto.response.StaffProfileResponse;
import com.gasmanagement.model.enums.UserRole;
import com.gasmanagement.security.UserDetailsImpl;
import com.gasmanagement.service.StaffService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/staff")
@RequiredArgsConstructor
public class StaffController {

    private final StaffService staffService;

    @PostMapping("/{userId}/profile")
    @PreAuthorize("hasAnyRole('MANAGER', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<StaffProfileResponse>> createStaffProfile(
            @PathVariable String userId) {
        StaffProfileResponse profile = staffService.createStaffProfile(userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Tạo staff profile thành công", profile));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<List<StaffProfileResponse>>> getAllStaff() {
        return ResponseEntity.ok(ApiResponse.ok(staffService.getAllStaff()));
    }

    @GetMapping("/available")
    @PreAuthorize("hasAnyRole('MANAGER', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<List<StaffProfileResponse>>> getAvailableStaff() {
        return ResponseEntity.ok(ApiResponse.ok(staffService.getAvailableStaff()));
    }

    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('STAFF', 'MANAGER', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<StaffProfileResponse>> getMyStaffProfile(
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.ok(ApiResponse.ok(staffService.getStaffByUserId(currentUser.getId())));
    }

    @PutMapping("/me")
    @PreAuthorize("hasAnyRole('STAFF', 'MANAGER', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<StaffProfileResponse>> updateMyStaffProfile(
            @RequestBody java.util.Map<String, String> body,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        String fullName = body.get("fullName");
        String phone = body.get("phone");
        String vehicleInfo = body.get("vehicleInfo");
        StaffProfileResponse updated = staffService.updateStaffInfo(
                currentUser.getId(), fullName, phone, vehicleInfo, null);
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật thông tin tài xế & phương tiện thành công", updated));
    }

    @GetMapping("/{userId}")
    @PreAuthorize("hasAnyRole('MANAGER', 'SYSTEM_ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<StaffProfileResponse>> getStaffByUserId(
            @PathVariable String userId) {
        return ResponseEntity.ok(ApiResponse.ok(staffService.getStaffByUserId(userId)));
    }

    @PutMapping("/{userId}")
    @PreAuthorize("hasAnyRole('MANAGER', 'SYSTEM_ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<StaffProfileResponse>> updateStaffInfo(
            @PathVariable String userId,
            @RequestBody java.util.Map<String, String> body,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        boolean isStaffOnly = currentUser != null
                && currentUser.getAuthorities().stream().anyMatch(a -> "ROLE_STAFF".equals(a.getAuthority()))
                && currentUser.getAuthorities().stream().noneMatch(a -> "ROLE_MANAGER".equals(a.getAuthority()) || "ROLE_SYSTEM_ADMIN".equals(a.getAuthority()));
        if (isStaffOnly && !currentUser.getId().equals(userId)) {
            throw new AccessDeniedException("Bạn chỉ có thể cập nhật thông tin của chính mình");
        }
        String fullName = body.get("fullName");
        String phone = body.get("phone");
        String vehicleInfo = body.get("vehicleInfo");
        String statusStr = body.get("staffStatus");
        com.gasmanagement.model.enums.StaffStatus staffStatus = null;
        if (statusStr != null && !statusStr.isBlank() && !isStaffOnly) {
            try {
                staffStatus = com.gasmanagement.model.enums.StaffStatus.valueOf(statusStr.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        }
        StaffProfileResponse updated = staffService.updateStaffInfo(userId, fullName, phone, vehicleInfo, staffStatus);
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật thông tin Shipper thành công", updated));
    }

    @PutMapping("/{userId}/vehicle")
    @PreAuthorize("hasAnyRole('MANAGER', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<StaffProfileResponse>> updateVehicleInfo(
            @PathVariable String userId,
            @RequestParam String vehicleInfo) {
        return ResponseEntity.ok(
                ApiResponse.ok("Cập nhật thông tin phương tiện thành công",
                        staffService.updateVehicleInfo(userId, vehicleInfo)));
    }

    @PutMapping("/{userId}/status")
    @PreAuthorize("hasAnyRole('STAFF', 'MANAGER', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<StaffProfileResponse>> updateStaffStatus(
            @PathVariable String userId,
            @Valid @RequestBody StaffStatusUpdateRequest request,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        if (currentUser != null && currentUser.getAuthorities().stream().anyMatch(a -> "ROLE_STAFF".equals(a.getAuthority()))
                && !currentUser.getId().equals(userId)) {
            throw new AccessDeniedException("Bạn chỉ có thể cập nhật trạng thái của chính mình");
        }
        return ResponseEntity.ok(
                ApiResponse.ok("Cập nhật trạng thái thành công",
                        staffService.updateStaffStatus(userId, request)));
    }

    @GetMapping("/{userId}/deliveries")
    @PreAuthorize("hasAnyRole('STAFF', 'MANAGER', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<List<DeliveryResponse>>> getDeliveryHistory(
            @PathVariable String userId,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        if (currentUser != null && currentUser.getAuthorities().stream().anyMatch(a -> "ROLE_STAFF".equals(a.getAuthority()))
                && !currentUser.getId().equals(userId)) {
            throw new AccessDeniedException("Bạn chỉ có thể xem lịch sử giao hàng của chính mình");
        }
        return ResponseEntity.ok(ApiResponse.ok(staffService.getDeliveryHistory(userId)));
    }
}
