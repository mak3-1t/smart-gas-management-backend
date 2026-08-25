package com.gasmanagement.controller;

import com.gasmanagement.dto.response.ApiResponse;
import com.gasmanagement.security.UserDetailsImpl;
import com.gasmanagement.service.OrderApprovalService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/manager/orders")
@RequiredArgsConstructor
@PreAuthorize("hasRole('MANAGER')")
public class OrderApprovalController {

    private final OrderApprovalService orderApprovalService;

    @PostMapping("/{orderId}/approve")
    public ResponseEntity<ApiResponse<String>> approveOrder(
            @PathVariable String orderId,
            @RequestParam(required = false) String note,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        
        orderApprovalService.approveOrder(orderId, userDetails.getId(), note);
        return ResponseEntity.ok(ApiResponse.ok("Đã duyệt đơn hàng " + orderId));
    }

    @PostMapping("/{orderId}/reject")
    public ResponseEntity<ApiResponse<String>> rejectOrder(
            @PathVariable String orderId,
            @RequestParam(required = false) String note,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        
        orderApprovalService.rejectOrder(orderId, userDetails.getId(), note);
        return ResponseEntity.ok(ApiResponse.ok("Đã từ chối đơn hàng " + orderId));
    }
}
