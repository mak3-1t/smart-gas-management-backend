package com.gasmanagement.service.impl;

import com.gasmanagement.dto.request.ReviewRequest;
import com.gasmanagement.dto.response.ReviewResponse;
import com.gasmanagement.exception.BusinessException;
import com.gasmanagement.exception.ResourceNotFoundException;
import com.gasmanagement.model.CustomerProfile;
import com.gasmanagement.model.Order;
import com.gasmanagement.model.Review;
import com.gasmanagement.model.User;
import com.gasmanagement.model.enums.OrderStatus;
import com.gasmanagement.repository.CustomerProfileRepository;
import com.gasmanagement.repository.OrderRepository;
import com.gasmanagement.repository.ReviewRepository;
import com.gasmanagement.repository.UserRepository;
import com.gasmanagement.service.ReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final OrderRepository orderRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final UserRepository userRepository;

    @Override
    public ReviewResponse createReview(String customerId, ReviewRequest request) {
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng: " + request.getOrderId()));

        if (!order.getCustomerId().equals(customerId)) {
            throw new BusinessException("Bạn chỉ có thể đánh giá đơn hàng của chính mình");
        }

        if (order.getOrderStatus() != OrderStatus.DELIVERED) {
            throw new BusinessException("Chỉ có thể đánh giá sau khi đơn hàng đã được giao thành công");
        }

        if (reviewRepository.existsByOrderIdAndCustomerId(request.getOrderId(), customerId)) {
            throw new BusinessException("Bạn đã đánh giá đơn hàng này rồi");
        }

        CustomerProfile profile = customerProfileRepository.findByUserId(customerId).orElse(null);
        User user = userRepository.findById(customerId).orElse(null);
        String customerName = profile != null ? profile.getFullName() : (user != null ? user.getUsername() : "Khách hàng");

        String productId = request.getProductId();
        if ((productId == null || productId.isBlank()) && order.getItems() != null && !order.getItems().isEmpty()) {
            productId = order.getItems().get(0).getProductId();
        }

        Review review = Review.builder()
                .orderId(order.getId())
                .productId(productId)
                .customerId(customerId)
                .customerName(customerName)
                .rating(request.getRating())
                .comment(request.getComment())
                .deliveryRating(request.getDeliveryRating())
                .deliveryComment(request.getDeliveryComment())
                .createdAt(LocalDateTime.now())
                .build();

        Review saved = reviewRepository.save(review);
        log.info("Khách hàng {} đã đánh giá đơn hàng {}", customerId, order.getId());
        return mapToResponse(saved);
    }

    @Override
    public List<ReviewResponse> getProductReviews(String productId) {
        return reviewRepository.findByProductId(productId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public ReviewResponse getOrderReview(String orderId) {
        Review review = reviewRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Đơn hàng này chưa có đánh giá"));
        return mapToResponse(review);
    }

    private ReviewResponse mapToResponse(Review r) {
        return ReviewResponse.builder()
                .id(r.getId())
                .orderId(r.getOrderId())
                .productId(r.getProductId())
                .customerId(r.getCustomerId())
                .customerName(r.getCustomerName())
                .rating(r.getRating())
                .comment(r.getComment())
                .deliveryRating(r.getDeliveryRating())
                .deliveryComment(r.getDeliveryComment())
                .createdAt(r.getCreatedAt())
                .build();
    }
}
