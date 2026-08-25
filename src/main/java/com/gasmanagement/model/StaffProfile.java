package com.gasmanagement.model;

import com.gasmanagement.model.enums.StaffStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "staff_profiles")
public class StaffProfile {

    @Id
    private String id;

    @Indexed(unique = true)
    private String userId;

    @Builder.Default
    private StaffStatus staffStatus = StaffStatus.OFFLINE;

    /** Batch/Route Delivery: danh sách Delivery Job đang active cùng lúc */
    private List<String> activeDeliveryIds;

    private String vehicleInfo;

    @Builder.Default
    private int totalDeliveries = 0;

    @Builder.Default
    private int successDeliveries = 0;

    @Builder.Default
    private int failedDeliveries = 0;
}
