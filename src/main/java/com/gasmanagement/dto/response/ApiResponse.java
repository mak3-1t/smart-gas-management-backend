package com.gasmanagement.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

/**
 * Response wrapper chuẩn cho toàn bộ API.
 *
 * <pre>
 * {
 *   "success": true,
 *   "message": "Thành công",
 *   "data": { ... }
 * }
 * </pre>
 *
 * @param <T> kiểu dữ liệu của field data
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private final boolean success;
    private final String message;
    private final T data;

    // ─── Factory helpers ───────────────────────────────────────────────

    public static <T> ApiResponse<T> ok(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message("Thành công")
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> ok(String message, T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .build();
    }
}
