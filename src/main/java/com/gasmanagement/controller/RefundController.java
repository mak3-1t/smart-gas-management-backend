package com.gasmanagement.controller;

import com.gasmanagement.dto.request.RefundProcessRequest;
import com.gasmanagement.dto.response.ApiResponse;
import com.gasmanagement.dto.response.RefundResponse;
import com.gasmanagement.model.enums.RefundStatus;
import com.gasmanagement.security.UserDetailsImpl;
import com.gasmanagement.service.RefundService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/manager/refunds")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('MANAGER', 'SYSTEM_ADMIN')")
public class RefundController {

    private final RefundService refundService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<RefundResponse>>> getAllRefunds(
            @RequestParam(required = false) RefundStatus status) {
        return ResponseEntity.ok(ApiResponse.ok(refundService.getAllRefunds(status)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RefundResponse>> getRefundById(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.ok(refundService.getRefundById(id)));
    }

    @PostMapping("/{id}/process")
    public ResponseEntity<ApiResponse<RefundResponse>> processRefund(
            @PathVariable String id,
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody RefundProcessRequest request) {
        RefundResponse response = refundService.processRefund(id, userDetails.getId(), request);
        return ResponseEntity.ok(ApiResponse.ok("Xử lý yêu cầu hoàn tiền thành công", response));
    }
}
