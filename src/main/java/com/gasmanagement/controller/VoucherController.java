package com.gasmanagement.controller;

import com.gasmanagement.dto.request.VoucherRequest;
import com.gasmanagement.dto.response.ApiResponse;
import com.gasmanagement.dto.response.VoucherResponse;
import com.gasmanagement.service.VoucherService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller quản lý Voucher.
 * <p>
 * - Manager: CRUD Voucher.
 * - Customer: Lấy danh sách Voucher hợp lệ, Tính toán thử số tiền giảm giá.
 */
@RestController
@RequestMapping("/api/vouchers")
@RequiredArgsConstructor
public class VoucherController {

    private final VoucherService voucherService;

    // ───────────────── MANAGER ENDPOINTS ─────────────────

    @PostMapping
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<VoucherResponse>> createVoucher(
            @Valid @RequestBody VoucherRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(voucherService.createVoucher(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<VoucherResponse>> updateVoucher(
            @PathVariable String id,
            @Valid @RequestBody VoucherRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(voucherService.updateVoucher(id, request)));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<VoucherResponse>> changeStatus(
            @PathVariable String id,
            @RequestParam String status) {
        return ResponseEntity.ok(ApiResponse.ok(voucherService.changeStatus(id, status)));
    }

    @GetMapping
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<Page<VoucherResponse>>> getAllVouchers(
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(voucherService.getAllVouchers(status, pageable)));
    }


    // ───────────────── CUSTOMER ENDPOINTS ─────────────────

    @GetMapping("/usable")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<List<VoucherResponse>>> getUsableVouchers() {
        return ResponseEntity.ok(ApiResponse.ok(voucherService.getUsableVouchers()));
    }

    @GetMapping("/calculate-discount")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<Double>> calculateDiscount(
            @RequestParam String code,
            @RequestParam Double orderTotal) {
        return ResponseEntity.ok(ApiResponse.ok(voucherService.calculateDiscount(code, orderTotal)));
    }
}
