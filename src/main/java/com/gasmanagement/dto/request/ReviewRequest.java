package com.gasmanagement.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewRequest {

    @NotBlank(message = "Order ID không được để trống")
    private String orderId;

    private String productId;

    @Min(value = 1, message = "Đánh giá sản phẩm tối thiểu là 1 sao")
    @Max(value = 5, message = "Đánh giá sản phẩm tối đa là 5 sao")
    private int rating;

    private String comment;

    @Min(value = 1, message = "Đánh giá giao hàng tối thiểu là 1 sao")
    @Max(value = 5, message = "Đánh giá giao hàng tối đa là 5 sao")
    private int deliveryRating;

    private String deliveryComment;
}
