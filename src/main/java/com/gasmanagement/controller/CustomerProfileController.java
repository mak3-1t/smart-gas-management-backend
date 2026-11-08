package com.gasmanagement.controller;

import com.gasmanagement.dto.request.UpdateProfileRequest;
import com.gasmanagement.dto.response.ApiResponse;
import com.gasmanagement.dto.response.CustomerProfileResponse;
import com.gasmanagement.security.UserDetailsImpl;
import com.gasmanagement.service.CustomerProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customer/profile")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CUSTOMER')")
public class CustomerProfileController {

    private final CustomerProfileService customerProfileService;

    @GetMapping
    public ResponseEntity<ApiResponse<CustomerProfileResponse>> getProfile(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        CustomerProfileResponse response = customerProfileService.getProfile(userDetails.getId());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<CustomerProfileResponse>> updateProfile(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody UpdateProfileRequest request) {
        CustomerProfileResponse response = customerProfileService.updateProfile(userDetails.getId(), request);
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật thông tin thành công", response));
    }
}
