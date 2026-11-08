package com.gasmanagement.service.impl;

import com.gasmanagement.dto.request.UpdateProfileRequest;
import com.gasmanagement.dto.response.CustomerProfileResponse;
import com.gasmanagement.exception.ResourceNotFoundException;
import com.gasmanagement.model.CustomerProfile;
import com.gasmanagement.model.User;
import com.gasmanagement.repository.CustomerProfileRepository;
import com.gasmanagement.repository.UserRepository;
import com.gasmanagement.service.CustomerProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerProfileServiceImpl implements CustomerProfileService {

    private final CustomerProfileRepository customerProfileRepository;
    private final UserRepository userRepository;

    @Override
    public CustomerProfileResponse getProfile(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));

        CustomerProfile profile = customerProfileRepository.findByUserId(userId)
                .orElseGet(() -> customerProfileRepository.save(CustomerProfile.builder()
                        .userId(userId)
                        .fullName(user.getUsername())
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build()));

        return mapToResponse(user, profile);
    }

    @Override
    public CustomerProfileResponse updateProfile(String userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));

        CustomerProfile profile = customerProfileRepository.findByUserId(userId)
                .orElseGet(() -> CustomerProfile.builder()
                        .userId(userId)
                        .createdAt(LocalDateTime.now())
                        .build());

        if (request.getFullName() != null) {
            profile.setFullName(request.getFullName());
        }
        if (request.getAvatarUrl() != null) {
            profile.setAvatarUrl(request.getAvatarUrl());
        }
        if (request.getDateOfBirth() != null) {
            profile.setDateOfBirth(request.getDateOfBirth());
        }
        profile.setUpdatedAt(LocalDateTime.now());
        customerProfileRepository.save(profile);

        if (request.getPhone() != null && !request.getPhone().equals(user.getPhone())) {
            user.setPhone(request.getPhone());
            user.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user);
        }

        log.info("Cập nhật thông tin khách hàng: userId={}", userId);
        return mapToResponse(user, profile);
    }

    private CustomerProfileResponse mapToResponse(User user, CustomerProfile profile) {
        return CustomerProfileResponse.builder()
                .id(profile != null ? profile.getId() : null)
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole())
                .status(user.getStatus())
                .fullName(profile != null ? profile.getFullName() : user.getUsername())
                .avatarUrl(profile != null ? profile.getAvatarUrl() : null)
                .dateOfBirth(profile != null ? profile.getDateOfBirth() : null)
                .createdAt(profile != null ? profile.getCreatedAt() : user.getCreatedAt())
                .updatedAt(profile != null ? profile.getUpdatedAt() : user.getUpdatedAt())
                .build();
    }
}
