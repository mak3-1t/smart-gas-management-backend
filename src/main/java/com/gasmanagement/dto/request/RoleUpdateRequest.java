package com.gasmanagement.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Request để System Admin thay đổi Role của User.
 */
@Data
public class RoleUpdateRequest {

    @NotNull(message = "Role không được để trống")
    private String role;   // CUSTOMER | STAFF | MANAGER | SYSTEM_ADMIN

    private String reason;
}
