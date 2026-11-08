package com.gasmanagement.dto.response;

import com.gasmanagement.model.enums.AccountStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerSummaryResponse {

    private String id;
    private String userId;
    private String username;
    private String fullName;
    private String email;
    private String phone;
    private AccountStatus status;
    private String address;

    private long totalOrders;
    private long deliveredOrders;
    private double totalSpent;
    private LocalDateTime lastOrderDate;
    private LocalDateTime createdAt;
}

