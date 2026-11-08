package com.gasmanagement.controller;

import com.gasmanagement.dto.request.CancelOrderRequest;
import com.gasmanagement.dto.request.CreateOrderRequest;
import com.gasmanagement.dto.response.ApiResponse;
import com.gasmanagement.dto.response.OrderResponse;
import com.gasmanagement.model.enums.ApprovalStatus;
import com.gasmanagement.model.enums.OrderStatus;
import com.gasmanagement.security.UserDetailsImpl;
import com.gasmanagement.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody CreateOrderRequest request) {
        OrderResponse response = orderService.createOrder(userDetails.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Đặt hàng thành công", response));
    }

    @GetMapping("/my-orders")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> getMyOrders(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) ApprovalStatus approvalStatus,
            @PageableDefault(size = 10) Pageable pageable) {
        Page<OrderResponse> page = orderService.getMyOrders(userDetails.getId(), status, approvalStatus, pageable);
        return ResponseEntity.ok(ApiResponse.ok(page));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderById(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable String id) {
        boolean isManagerOrStaff = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_MANAGER")
                        || a.getAuthority().equals("ROLE_STAFF")
                        || a.getAuthority().equals("ROLE_SYSTEM_ADMIN"));

        OrderResponse response = orderService.getOrderById(id, userDetails.getId(), isManagerOrStaff);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelOrder(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable String id,
            @Valid @RequestBody CancelOrderRequest request) {
        OrderResponse response = orderService.cancelOrder(id, userDetails.getId(), request);
        return ResponseEntity.ok(ApiResponse.ok("Đã hủy đơn hàng thành công", response));
    }

    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('MANAGER', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> getAllOrders(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) ApprovalStatus approvalStatus,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<OrderResponse> page = orderService.getAllOrdersForManager(status, approvalStatus, pageable);
        return ResponseEntity.ok(ApiResponse.ok(page));
    }
}
