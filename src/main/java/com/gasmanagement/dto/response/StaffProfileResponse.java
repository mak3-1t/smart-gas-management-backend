package com.gasmanagement.dto.response;

import com.gasmanagement.model.enums.StaffStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO cho StaffProfile.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffProfileResponse {

    private String id;
    private String userId;
    private String username;
    private String email;
    private String phone;

    private StaffStatus staffStatus;
    private String vehicleInfo;

    private List<String> activeDeliveryIds;

    private int totalDeliveries;
    private int successDeliveries;
    private int failedDeliveries;

    /** Tỉ lệ giao hàng thành công (%) */
    private double successRate;
}
