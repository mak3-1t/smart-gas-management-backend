package com.gasmanagement.service.impl;

import com.gasmanagement.dto.request.ChangePasswordRequest;
import com.gasmanagement.dto.request.LoginRequest;
import com.gasmanagement.dto.request.RefreshTokenRequest;
import com.gasmanagement.dto.request.RegisterRequest;
import com.gasmanagement.dto.response.AuthResponse;
import com.gasmanagement.exception.BusinessException;
import com.gasmanagement.exception.ResourceNotFoundException;
import com.gasmanagement.model.CustomerProfile;
import com.gasmanagement.model.User;
import com.gasmanagement.model.enums.AccountStatus;
import com.gasmanagement.model.enums.UserRole;
import com.gasmanagement.repository.CustomerProfileRepository;
import com.gasmanagement.repository.UserRepository;
import com.gasmanagement.security.JwtService;
import com.gasmanagement.security.UserDetailsServiceImpl;
import com.gasmanagement.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserDetailsServiceImpl userDetailsService;

    @Override
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException("Username '" + request.getUsername() + "' đã được sử dụng");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("Email '" + request.getEmail() + "' đã được sử dụng");
        }

        // Bảo mật RBAC: Đăng ký công khai chỉ được phép tạo tài khoản CUSTOMER
        if (request.getRole() != null && request.getRole() != UserRole.CUSTOMER) {
            throw new BusinessException("Chỉ khách hàng (CUSTOMER) mới được tự đăng ký tài khoản. Tài khoản nội bộ do Quản trị viên cấp.");
        }

        LocalDateTime now = LocalDateTime.now();
        UserRole assignedRole = UserRole.CUSTOMER;

        User user = User.builder()
                .username(request.getUsername().trim())
                .email(request.getEmail().trim())
                .phone(request.getPhone())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(assignedRole)
                .status(AccountStatus.ACTIVE)
                .createdAt(now)
                .updatedAt(now)
                .build();

        User savedUser = userRepository.save(user);

        CustomerProfile profile = CustomerProfile.builder()
                .userId(savedUser.getId())
                .fullName(request.getFullName() != null && !request.getFullName().trim().isEmpty()
                        ? request.getFullName().trim() : request.getUsername().trim())
                .createdAt(now)
                .updatedAt(now)
                .build();
        customerProfileRepository.save(profile);

        log.info("Khách hàng mới đăng ký tài khoản: username={}, role={}", savedUser.getUsername(), assignedRole);

        UserDetails userDetails = userDetailsService.loadUserByUsername(savedUser.getUsername());
        String accessToken = jwtService.generateToken(userDetails);
        String refreshToken = jwtService.generateRefreshToken(userDetails);

        return buildAuthResponse(savedUser, profile, accessToken, refreshToken);
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsernameOrEmail(),
                            request.getPassword()
                    )
            );
        } catch (BadCredentialsException e) {
            throw new BusinessException("Tên đăng nhập hoặc mật khẩu không chính xác");
        }

        User user = userRepository.findByUsername(request.getUsernameOrEmail())
                .or(() -> userRepository.findByEmail(request.getUsernameOrEmail()))
                .orElseThrow(() -> new ResourceNotFoundException("Tài khoản không tồn tại"));

        if (user.getStatus() == AccountStatus.LOCKED) {
            throw new BusinessException("Tài khoản đã bị tạm khóa. Vui lòng liên hệ hỗ trợ.");
        }
        if (user.getStatus() == AccountStatus.DISABLED) {
            throw new BusinessException("Tài khoản đã bị vô hiệu hóa.");
        }

        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());
        String accessToken = jwtService.generateToken(userDetails);
        String refreshToken = jwtService.generateRefreshToken(userDetails);

        CustomerProfile profile = customerProfileRepository.findByUserId(user.getId()).orElse(null);

        log.info("User logged in: username={}, role={}", user.getUsername(), user.getRole());
        return buildAuthResponse(user, profile, accessToken, refreshToken);
    }

    @Override
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();
        String username;
        try {
            username = jwtService.extractUsername(refreshToken);
        } catch (Exception e) {
            throw new BusinessException("Refresh token không hợp lệ hoặc đã hết hạn");
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        if (!jwtService.isTokenValid(refreshToken, userDetails)) {
            throw new BusinessException("Refresh token không hợp lệ hoặc đã hết hạn");
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User không tồn tại"));

        String newAccessToken = jwtService.generateToken(userDetails);
        CustomerProfile profile = customerProfileRepository.findByUserId(user.getId()).orElse(null);

        return buildAuthResponse(user, profile, newAccessToken, refreshToken);
    }

    @Override
    public void changePassword(String userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
            throw new BusinessException("Mật khẩu hiện tại không đúng");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        log.info("User {} changed password successfully", userId);
    }

    @Override
    public AuthResponse getCurrentUser(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));
        CustomerProfile profile = customerProfileRepository.findByUserId(user.getId()).orElse(null);
        return buildAuthResponse(user, profile, null, null);
    }

    private AuthResponse buildAuthResponse(User user, CustomerProfile profile, String accessToken, String refreshToken) {
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole())
                .status(user.getStatus())
                .fullName(profile != null ? profile.getFullName() : user.getUsername())
                .avatarUrl(profile != null ? profile.getAvatarUrl() : null)
                .build();
    }
}
