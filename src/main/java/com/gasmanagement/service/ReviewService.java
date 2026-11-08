package com.gasmanagement.service;

import com.gasmanagement.dto.request.ReviewRequest;
import com.gasmanagement.dto.response.ReviewResponse;

import java.util.List;

public interface ReviewService {
    ReviewResponse createReview(String customerId, ReviewRequest request);
    List<ReviewResponse> getProductReviews(String productId);
    ReviewResponse getOrderReview(String orderId);
}
