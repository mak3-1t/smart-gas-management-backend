package com.gasmanagement.service.impl;

import com.gasmanagement.dto.request.StaffStatusUpdateRequest;
import com.gasmanagement.dto.response.DeliveryResponse;
import com.gasmanagement.dto.response.StaffProfileResponse;
import com.gasmanagement.exception.ResourceNotFoundException;
import com.gasmanagement.model.StaffProfile;
import com.gasmanagement.model.User;
import com.gasmanagement.model.enums.StaffStatus;
import com.gasmanagement.repository.DeliveryRepository;
import com.gasmanagement.repository.StaffProfileRepository;
import com.gasmanagement.repository.UserRepository;
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
    private final DeliveryRepository deliveryRepository;

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
        StaffProfile profile = findProfileOrThrow(userId);
        User user = userRepository.findById(profile.getUserId()).orElse(null);
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
        return deliveryRepository.findByStaffId(profile.getUserId()).stream()
                .map(d -> DeliveryResponse.builder()
                        .id(d.getId())
                        .orderId(d.getOrderId())
                        .staffId(d.getStaffId())
                        .deliveryStatus(d.getDeliveryStatus())
                        .batchId(d.getBatchId())
                        .deliverySequence(d.getDeliverySequence())
                        .failureReason(d.getFailureReason())
                        .failureNote(d.getFailureNote())
                        .assignedAt(d.getAssignedAt())
                        .acceptedAt(d.getAcceptedAt())
                        .startedAt(d.getStartedAt())
                        .deliveredAt(d.getDeliveredAt())
                        .statusHistory(d.getStatusHistory())
                        .createdAt(d.getCreatedAt())
                        .updatedAt(d.getUpdatedAt())
                        .build())
                .collect(Collectors.toList());
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
        double successRate = profile.getTotalDeliveries() == 0 ? 0.0 :
                (double) profile.getSuccessDeliveries() / profile.getTotalDeliveries() * 100;

        return StaffProfileResponse.builder()
                .id(profile.getId())
                .userId(profile.getUserId())
                .username(user != null ? user.getUsername() : null)
                .email(user != null ? user.getEmail() : null)
                .phone(user != null ? user.getPhone() : null)
                .staffStatus(profile.getStaffStatus())
                .vehicleInfo(profile.getVehicleInfo())
                .activeDeliveryIds(profile.getActiveDeliveryIds())
                .totalDeliveries(profile.getTotalDeliveries())
                .successDeliveries(profile.getSuccessDeliveries())
                .failedDeliveries(profile.getFailedDeliveries())
                .successRate(Math.round(successRate * 10.0) / 10.0)
                .build();
    }
}
