package com.gasmanagement.controller;

import com.gasmanagement.dto.request.AddressRequest;
import com.gasmanagement.dto.response.AddressResponse;
import com.gasmanagement.dto.response.ApiResponse;
import com.gasmanagement.security.UserDetailsImpl;
import com.gasmanagement.service.AddressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customer/addresses")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CUSTOMER')")
public class AddressController {

    private final AddressService addressService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<AddressResponse>>> getMyAddresses(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(ApiResponse.ok(addressService.getMyAddresses(userDetails.getId())));
    }

    @GetMapping("/default")
    public ResponseEntity<ApiResponse<AddressResponse>> getDefaultAddress(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(ApiResponse.ok(addressService.getDefaultAddress(userDetails.getId())));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AddressResponse>> getAddressById(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.ok(addressService.getAddressById(id, userDetails.getId())));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AddressResponse>> createAddress(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody AddressRequest request) {
        AddressResponse response = addressService.createAddress(userDetails.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Thêm địa chỉ thành công", response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<AddressResponse>> updateAddress(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable String id,
            @Valid @RequestBody AddressRequest request) {
        AddressResponse response = addressService.updateAddress(id, userDetails.getId(), request);
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật địa chỉ thành công", response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> deleteAddress(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable String id) {
        addressService.deleteAddress(id, userDetails.getId());
        return ResponseEntity.ok(ApiResponse.ok("Đã xóa địa chỉ"));
    }

    @PatchMapping("/{id}/default")
    public ResponseEntity<ApiResponse<AddressResponse>> setDefaultAddress(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable String id) {
        AddressResponse response = addressService.setDefaultAddress(id, userDetails.getId());
        return ResponseEntity.ok(ApiResponse.ok("Đã đặt làm địa chỉ mặc định", response));
    }
}
