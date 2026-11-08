package com.gasmanagement.controller;

import com.gasmanagement.dto.request.AddToCartRequest;
import com.gasmanagement.dto.request.ApplyVoucherRequest;
import com.gasmanagement.dto.request.UpdateCartItemRequest;
import com.gasmanagement.dto.response.ApiResponse;
import com.gasmanagement.dto.response.CartResponse;
import com.gasmanagement.model.enums.PurchaseType;
import com.gasmanagement.security.UserDetailsImpl;
import com.gasmanagement.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CUSTOMER')")
public class CartController {

    private final CartService cartService;

    @GetMapping
    public ResponseEntity<ApiResponse<CartResponse>> getCart(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        CartResponse response = cartService.getCart(userDetails.getId());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/items")
    public ResponseEntity<ApiResponse<CartResponse>> addToCart(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody AddToCartRequest request) {
        CartResponse response = cartService.addToCart(userDetails.getId(), request);
        return ResponseEntity.ok(ApiResponse.ok("Đã thêm sản phẩm vào giỏ hàng", response));
    }

    @PutMapping("/items")
    public ResponseEntity<ApiResponse<CartResponse>> updateCartItem(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody UpdateCartItemRequest request) {
        CartResponse response = cartService.updateCartItem(userDetails.getId(), request);
        return ResponseEntity.ok(ApiResponse.ok("Đã cập nhật số lượng", response));
    }

    @DeleteMapping("/items")
    public ResponseEntity<ApiResponse<CartResponse>> removeCartItem(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestParam String productId,
            @RequestParam PurchaseType purchaseType) {
        CartResponse response = cartService.removeCartItem(userDetails.getId(), productId, purchaseType);
        return ResponseEntity.ok(ApiResponse.ok("Đã xóa sản phẩm khỏi giỏ hàng", response));
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<String>> clearCart(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        cartService.clearCart(userDetails.getId());
        return ResponseEntity.ok(ApiResponse.ok("Đã làm trống giỏ hàng"));
    }

    @PostMapping("/voucher")
    public ResponseEntity<ApiResponse<CartResponse>> applyVoucher(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody ApplyVoucherRequest request) {
        CartResponse response = cartService.applyVoucher(userDetails.getId(), request.getVoucherCode());
        return ResponseEntity.ok(ApiResponse.ok("Áp dụng mã giảm giá thành công", response));
    }

    @DeleteMapping("/voucher")
    public ResponseEntity<ApiResponse<CartResponse>> removeVoucher(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        CartResponse response = cartService.removeVoucher(userDetails.getId());
        return ResponseEntity.ok(ApiResponse.ok("Đã gỡ mã giảm giá", response));
    }
}
