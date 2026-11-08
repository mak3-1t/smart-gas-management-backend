package com.gasmanagement.service;

import com.gasmanagement.dto.request.UpdateProfileRequest;
import com.gasmanagement.dto.response.CustomerProfileResponse;

public interface CustomerProfileService {
    CustomerProfileResponse getProfile(String userId);
    CustomerProfileResponse updateProfile(String userId, UpdateProfileRequest request);
}
