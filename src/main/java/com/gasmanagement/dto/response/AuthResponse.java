package com.gasmanagement.dto.response;

import com.gasmanagement.model.enums.AccountStatus;
import com.gasmanagement.model.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private String accessToken;
    private String refreshToken;

    @Builder.Default
    private String tokenType = "Bearer";

    private String userId;
    private String username;
    private String email;
    private String phone;
    private UserRole role;
    private AccountStatus status;

    private String fullName;
    private String avatarUrl;
}
