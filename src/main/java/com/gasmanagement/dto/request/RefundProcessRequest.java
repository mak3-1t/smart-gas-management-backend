package com.gasmanagement.dto.request;

import com.gasmanagement.model.enums.RefundStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundProcessRequest {

    @NotNull(message = "Trạng thái hoàn tiền không được để trống")
    private RefundStatus status;

    private String note;
}
