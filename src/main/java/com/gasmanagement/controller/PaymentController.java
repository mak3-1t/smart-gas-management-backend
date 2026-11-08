package com.gasmanagement.controller;

import com.gasmanagement.dto.request.PaymentProofRequest;
import com.gasmanagement.dto.response.ApiResponse;
import com.gasmanagement.dto.response.PaymentResponse;
import com.gasmanagement.security.UserDetailsImpl;
import com.gasmanagement.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPaymentById(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.ok(paymentService.getPaymentById(id)));
    }

    @GetMapping("/order/{orderId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPaymentByOrderId(@PathVariable String orderId) {
        return ResponseEntity.ok(ApiResponse.ok(paymentService.getPaymentByOrderId(orderId)));
    }

    /**
     * Xác nhận đã nhận tiền (Dành cho Staff khi thu tiền COD hoặc Manager xác nhận).
     * Endpoint tích hợp cốt lõi giữa DEV 2 và DEV 1:
     * POST /api/payments/{paymentId}/confirm
     */
    @PostMapping("/{paymentId}/confirm")
    @PreAuthorize("hasAnyRole('STAFF', 'MANAGER')")
    public ResponseEntity<ApiResponse<PaymentResponse>> confirmPayment(
            @PathVariable String paymentId,
            @RequestParam(required = false) String note,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        PaymentResponse response = paymentService.confirmPayment(paymentId, userDetails.getId(), note);
        return ResponseEntity.ok(ApiResponse.ok("Xác nhận thanh toán thành công", response));
    }

    /**
     * Khách hàng tải lên ảnh biên lai chuyển khoản.
     */
    @PostMapping("/{paymentId}/proof")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<PaymentResponse>> uploadProof(
            @PathVariable String paymentId,
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody PaymentProofRequest request) {
        PaymentResponse response = paymentService.uploadPaymentProof(paymentId, userDetails.getId(), request);
        return ResponseEntity.ok(ApiResponse.ok("Tải lên biên lai thành công. Đang chờ xác minh.", response));
    }

    /**
     * Manager duyệt hoặc từ chối biên lai chuyển khoản.
     */
    @PostMapping("/{paymentId}/verify")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<PaymentResponse>> verifyPayment(
            @PathVariable String paymentId,
            @RequestParam boolean approved,
            @RequestParam(required = false) String note,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        PaymentResponse response = paymentService.verifyPayment(paymentId, userDetails.getId(), approved, note);
        return ResponseEntity.ok(ApiResponse.ok("Đã cập nhật trạng thái thanh toán", response));
    }
}
