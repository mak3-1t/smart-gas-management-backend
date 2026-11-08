package com.gasmanagement.service;

import com.gasmanagement.dto.request.ChangePasswordRequest;
import com.gasmanagement.dto.request.LoginRequest;
import com.gasmanagement.dto.request.RefreshTokenRequest;
import com.gasmanagement.dto.request.RegisterRequest;
import com.gasmanagement.dto.response.AuthResponse;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    AuthResponse refreshToken(RefreshTokenRequest request);
    void changePassword(String userId, ChangePasswordRequest request);
    AuthResponse getCurrentUser(String userId);
}
