package com.gasmanagement.dto.request;

import com.gasmanagement.model.enums.DiscountType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class VoucherRequest {

    @NotBlank(message = "Code không được để trống")
    private String code;

    @NotNull(message = "Loại giảm giá không được để trống")
    private DiscountType discountType; // FIXED_AMOUNT, PERCENTAGE

    @NotNull(message = "Giá trị giảm không được để trống")
    @Min(value = 0, message = "Giá trị giảm phải lớn hơn hoặc bằng 0")
    private Double discountValue;

    @NotNull(message = "Giá trị đơn tối thiểu không được để trống")
    @Min(value = 0, message = "Giá trị đơn tối thiểu phải lớn hơn hoặc bằng 0")
    private Double minimumOrder;

    private Double maximumDiscount; // Dành cho PERCENTAGE

    @NotNull(message = "Ngày bắt đầu không được để trống")
    private LocalDateTime startDate;

    @NotNull(message = "Ngày kết thúc không được để trống")
    private LocalDateTime endDate;

    @NotNull(message = "Giới hạn sử dụng không được để trống")
    @Min(value = 1, message = "Giới hạn sử dụng phải lớn hơn 0")
    private Integer usageLimit;
}
