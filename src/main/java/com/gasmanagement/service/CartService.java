package com.gasmanagement.service;

import com.gasmanagement.dto.request.AddToCartRequest;
import com.gasmanagement.dto.request.UpdateCartItemRequest;
import com.gasmanagement.dto.response.CartResponse;
import com.gasmanagement.model.enums.PurchaseType;

public interface CartService {
    CartResponse getCart(String userId);
    CartResponse addToCart(String userId, AddToCartRequest request);
    CartResponse updateCartItem(String userId, UpdateCartItemRequest request);
    CartResponse removeCartItem(String userId, String productId, PurchaseType purchaseType);
    void clearCart(String userId);
    CartResponse applyVoucher(String userId, String voucherCode);
    CartResponse removeVoucher(String userId);
}
