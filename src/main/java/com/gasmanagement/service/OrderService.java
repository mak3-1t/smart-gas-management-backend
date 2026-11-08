package com.gasmanagement.service;

import com.gasmanagement.dto.request.CancelOrderRequest;
import com.gasmanagement.dto.request.CreateOrderRequest;
import com.gasmanagement.dto.response.OrderResponse;
import com.gasmanagement.model.enums.ApprovalStatus;
import com.gasmanagement.model.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OrderService {

    OrderResponse createOrder(String customerId, CreateOrderRequest request);

    Page<OrderResponse> getMyOrders(String customerId, OrderStatus status, ApprovalStatus approvalStatus, Pageable pageable);

    OrderResponse getOrderById(String orderId, String userId, boolean isManagerOrStaff);

    OrderResponse cancelOrder(String orderId, String customerId, CancelOrderRequest request);

    Page<OrderResponse> getAllOrdersForManager(OrderStatus status, ApprovalStatus approvalStatus, Pageable pageable);
}
