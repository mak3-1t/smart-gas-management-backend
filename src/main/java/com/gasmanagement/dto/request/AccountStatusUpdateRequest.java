package com.gasmanagement.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Request để System Admin thay đổi trạng thái tài khoản.
 */
@Data
public class AccountStatusUpdateRequest {

    @NotNull(message = "Status không được để trống")
    private String status;   // ACTIVE | LOCKED | DISABLED

    /** Lý do thay đổi – bắt buộc khi LOCK hoặc DISABLE */
    private String reason;
}
