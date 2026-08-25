package com.gasmanagement.dto.request;

import com.gasmanagement.model.enums.StaffStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request để Staff tự update trạng thái làm việc.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffStatusUpdateRequest {

    @NotNull(message = "staffStatus không được để trống")
    private StaffStatus staffStatus; // AVAILABLE | OFFLINE (Staff không tự BUSY)
}
