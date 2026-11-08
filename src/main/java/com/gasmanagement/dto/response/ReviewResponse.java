package com.gasmanagement.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResponse {

    private String id;
    private String orderId;
    private String productId;
    private String customerId;
    private String customerName;
    private int rating;
    private String comment;
    private int deliveryRating;
    private String deliveryComment;
    private LocalDateTime createdAt;
}
