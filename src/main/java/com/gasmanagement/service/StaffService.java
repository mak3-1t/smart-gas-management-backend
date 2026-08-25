package com.gasmanagement.service;

import com.gasmanagement.dto.request.StaffStatusUpdateRequest;
import com.gasmanagement.dto.response.DeliveryResponse;
import com.gasmanagement.dto.response.StaffProfileResponse;

import java.util.List;

/**
 * Service quản lý Staff (Manager + Staff self-service).
 */
public interface StaffService {

    // ─── MANAGER ───

    /** Tạo StaffProfile khi Manager tạo tài khoản Staff mới */
    StaffProfileResponse createStaffProfile(String userId);

    /** Danh sách tất cả staff */
    List<StaffProfileResponse> getAllStaff();

    /** Staff đang AVAILABLE */
    List<StaffProfileResponse> getAvailableStaff();

    /** Thông tin 1 staff theo userId */
    StaffProfileResponse getStaffByUserId(String userId);

    /** Cập nhật vehicleInfo */
    StaffProfileResponse updateVehicleInfo(String userId, String vehicleInfo);

    // ─── STAFF SELF-SERVICE ───

    /** Staff tự cập nhật trạng thái (AVAILABLE / OFFLINE) */
    StaffProfileResponse updateStaffStatus(String userId, StaffStatusUpdateRequest request);

    /** Lịch sử delivery của staff */
    List<DeliveryResponse> getDeliveryHistory(String userId);
}
