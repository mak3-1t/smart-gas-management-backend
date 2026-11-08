package com.gasmanagement.dto.response;

import com.gasmanagement.model.enums.AccountStatus;
import com.gasmanagement.model.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerProfileResponse {
    private String id;
    private String userId;
    private String username;
    private String email;
    private String phone;
    private UserRole role;
    private AccountStatus status;

    private String fullName;
    private String avatarUrl;
    private LocalDate dateOfBirth;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
