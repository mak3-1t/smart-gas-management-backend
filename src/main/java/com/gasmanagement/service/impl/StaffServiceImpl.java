package com.gasmanagement.service.impl;

import com.gasmanagement.dto.request.StaffStatusUpdateRequest;
import com.gasmanagement.dto.response.DeliveryResponse;
import com.gasmanagement.dto.response.StaffProfileResponse;
import com.gasmanagement.exception.ResourceNotFoundException;
import com.gasmanagement.model.CustomerProfile;
import com.gasmanagement.model.Delivery;
import com.gasmanagement.model.StaffProfile;
import com.gasmanagement.model.User;
import com.gasmanagement.model.enums.DeliveryStatus;
import com.gasmanagement.model.enums.StaffStatus;
import com.gasmanagement.repository.CustomerProfileRepository;
import com.gasmanagement.repository.DeliveryRepository;
import com.gasmanagement.repository.StaffProfileRepository;
import com.gasmanagement.repository.UserRepository;
import com.gasmanagement.service.DeliveryService;
import com.gasmanagement.service.StaffService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StaffServiceImpl implements StaffService {

    private final StaffProfileRepository staffProfileRepository;
    private final UserRepository userRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final DeliveryRepository deliveryRepository;
    private final DeliveryService deliveryService;

    // ─────────────── MANAGER ───────────────

    @Override
    public StaffProfileResponse createStaffProfile(String userId) {
        // Kiểm tra user tồn tại
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy user với id: " + userId));

        // Kiểm tra chưa có profile
        if (staffProfileRepository.findByUserId(userId).isPresent()) {
            throw new IllegalStateException("Staff profile đã tồn tại cho userId: " + userId);
        }

        StaffProfile profile = StaffProfile.builder()
                .userId(userId)
                .staffStatus(StaffStatus.OFFLINE)
                .activeDeliveryIds(new ArrayList<>())
                .totalDeliveries(0)
                .successDeliveries(0)
                .failedDeliveries(0)
                .build();

        StaffProfile saved = staffProfileRepository.save(profile);
        log.info("Created staff profile for userId: {}", userId);
        return mapToResponse(saved, user);
    }

    @Override
    public List<StaffProfileResponse> getAllStaff() {
        return staffProfileRepository.findAll().stream()
                .map(this::mapToResponseWithUser)
                .collect(Collectors.toList());
    }

    @Override
    public List<StaffProfileResponse> getAvailableStaff() {
        return staffProfileRepository.findByStaffStatus(StaffStatus.AVAILABLE).stream()
                .map(this::mapToResponseWithUser)
                .collect(Collectors.toList());
    }

    @Override
    public StaffProfileResponse getStaffByUserId(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy user với id: " + userId));
        StaffProfile profile = staffProfileRepository.findByUserId(userId)
                .orElseGet(() -> staffProfileRepository.save(StaffProfile.builder()
                        .userId(userId)
                        .staffStatus(StaffStatus.AVAILABLE)
                        .vehicleInfo("Xe máy chuyên dụng")
                        .activeDeliveryIds(new ArrayList<>())
                        .totalDeliveries(0)
                        .successDeliveries(0)
                        .failedDeliveries(0)
                        .build()));
        return mapToResponse(profile, user);
    }

    @Override
    public StaffProfileResponse updateVehicleInfo(String userId, String vehicleInfo) {
        StaffProfile profile = findProfileOrThrow(userId);
        profile.setVehicleInfo(vehicleInfo);
        StaffProfile saved = staffProfileRepository.save(profile);
        User user = userRepository.findById(userId).orElse(null);
        return mapToResponse(saved, user);
    }

    @Override
    public StaffProfileResponse updateStaffInfo(String userId, String fullName, String phone, String vehicleInfo, StaffStatus staffStatus) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy user với id: " + userId));

        if (phone != null) {
            user.setPhone(phone.trim());
            user.setUpdatedAt(java.time.LocalDateTime.now());
            userRepository.save(user);
        }

        if (fullName != null && !fullName.isBlank()) {
            CustomerProfile custProfile = customerProfileRepository.findByUserId(userId)
                    .orElseGet(() -> CustomerProfile.builder()
                            .userId(userId)
                            .createdAt(java.time.LocalDateTime.now())
                            .build());
            custProfile.setFullName(fullName.trim());
            custProfile.setUpdatedAt(java.time.LocalDateTime.now());
            customerProfileRepository.save(custProfile);
        }

        StaffProfile profile = staffProfileRepository.findByUserId(userId)
                .orElseGet(() -> StaffProfile.builder()
                        .userId(userId)
                        .staffStatus(StaffStatus.AVAILABLE)
                        .vehicleInfo("Xe máy chuyên dụng")
                        .activeDeliveryIds(new ArrayList<>())
                        .totalDeliveries(0)
                        .successDeliveries(0)
                        .failedDeliveries(0)
                        .build());

        if (vehicleInfo != null && !vehicleInfo.isBlank()) {
            profile.setVehicleInfo(vehicleInfo.trim());
        }
        if (staffStatus != null) {
            profile.setStaffStatus(staffStatus);
        }

        StaffProfile saved = staffProfileRepository.save(profile);
        return mapToResponse(saved, user);
    }

    // ─────────────── STAFF SELF-SERVICE ───────────────

    @Override
    public StaffProfileResponse updateStaffStatus(String userId, StaffStatusUpdateRequest request) {
        StaffProfile profile = findProfileOrThrow(userId);

        // Staff chỉ được tự set AVAILABLE hoặc OFFLINE (không tự BUSY)
        if (request.getStaffStatus() == StaffStatus.BUSY) {
            throw new IllegalArgumentException(
                    "Staff không thể tự chuyển trạng thái sang BUSY. " +
                    "Trạng thái này được hệ thống tự động cập nhật khi nhận đơn.");
        }

        // Không cho OFFLINE nếu còn đơn đang active
        if (request.getStaffStatus() == StaffStatus.OFFLINE &&
                profile.getActiveDeliveryIds() != null &&
                !profile.getActiveDeliveryIds().isEmpty()) {
            throw new IllegalStateException(
                    "Không thể OFFLINE khi còn " +
                    profile.getActiveDeliveryIds().size() + " đơn đang giao.");
        }

        profile.setStaffStatus(request.getStaffStatus());
        StaffProfile saved = staffProfileRepository.save(profile);
        User user = userRepository.findById(userId).orElse(null);
        log.info("Staff {} updated status to {}", userId, request.getStaffStatus());
        return mapToResponse(saved, user);
    }

    @Override
    public List<DeliveryResponse> getDeliveryHistory(String userId) {
        StaffProfile profile = findProfileOrThrow(userId);
        return deliveryService.getDeliveriesByStaff(profile.getUserId());
    }

    // ─────────────── HELPERS ───────────────

    private StaffProfile findProfileOrThrow(String userId) {
        return staffProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy staff profile cho userId: " + userId));
    }

    private StaffProfileResponse mapToResponseWithUser(StaffProfile profile) {
        User user = userRepository.findById(profile.getUserId()).orElse(null);
        return mapToResponse(profile, user);
    }

    private StaffProfileResponse mapToResponse(StaffProfile profile, User user) {
        List<Delivery> staffDeliveries = deliveryRepository.findByStaffId(profile.getUserId());
        int realTotal = (int) staffDeliveries.stream()
                .filter(d -> d.getDeliveryStatus() == DeliveryStatus.DELIVERED
                        || d.getDeliveryStatus() == DeliveryStatus.FAILED)
                .count();
        int realSuccess = (int) staffDeliveries.stream()
                .filter(d -> d.getDeliveryStatus() == DeliveryStatus.DELIVERED)
                .count();
        int realFailed = (int) staffDeliveries.stream()
                .filter(d -> d.getDeliveryStatus() == DeliveryStatus.FAILED)
                .count();
        List<String> activeIds = staffDeliveries.stream()
                .filter(d -> d.getDeliveryStatus() == DeliveryStatus.WAITING
                        || d.getDeliveryStatus() == DeliveryStatus.ACCEPTED
                        || d.getDeliveryStatus() == DeliveryStatus.DELIVERING)
                .map(Delivery::getId)
                .collect(Collectors.toList());

        double successRate = realTotal == 0 ? 100.0 :
                (double) realSuccess / realTotal * 100.0;

        String fullName = customerProfileRepository.findByUserId(profile.getUserId())
                .map(CustomerProfile::getFullName)
                .filter(n -> n != null && !n.isBlank())
                .orElse(user != null ? user.getUsername() : null);

        return StaffProfileResponse.builder()
                .id(profile.getId())
                .userId(profile.getUserId())
                .username(user != null ? user.getUsername() : null)
                .fullName(fullName)
                .email(user != null ? user.getEmail() : null)
                .phone(user != null ? user.getPhone() : null)
                .staffStatus(profile.getStaffStatus())
                .vehicleInfo(profile.getVehicleInfo())
                .activeDeliveryIds(activeIds)
                .totalDeliveries(realTotal)
                .successDeliveries(realSuccess)
                .failedDeliveries(realFailed)
                .successRate(Math.round(successRate * 10.0) / 10.0)
                .build();
    }
}
