package com.gasmanagement.service.impl;

import com.gasmanagement.dto.response.CartResponse;
import com.gasmanagement.dto.request.AddToCartRequest;
import com.gasmanagement.dto.request.UpdateCartItemRequest;
import com.gasmanagement.exception.BusinessException;
import com.gasmanagement.exception.ResourceNotFoundException;
import com.gasmanagement.model.Brand;
import com.gasmanagement.model.Cart;
import com.gasmanagement.model.Inventory;
import com.gasmanagement.model.Product;
import com.gasmanagement.model.StoreSetting;
import com.gasmanagement.model.enums.ProductStatus;
import com.gasmanagement.model.enums.PurchaseType;
import com.gasmanagement.repository.BrandRepository;
import com.gasmanagement.repository.CartRepository;
import com.gasmanagement.repository.InventoryRepository;
import com.gasmanagement.repository.ProductRepository;
import com.gasmanagement.service.CartService;
import com.gasmanagement.service.StoreSettingService;
import com.gasmanagement.service.VoucherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final BrandRepository brandRepository;
    private final VoucherService voucherService;
    private final StoreSettingService storeSettingService;

    @Override
    public CartResponse getCart(String userId) {
        Cart cart = getOrCreateCart(userId);
        return buildCartResponse(cart);
    }

    @Override
    public CartResponse addToCart(String userId, AddToCartRequest request) {
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm không tồn tại"));

        if (product.getStatus() != ProductStatus.ACTIVE) {
            throw new BusinessException("Sản phẩm này hiện đang tạm ngưng kinh doanh");
        }

        Inventory inventory = inventoryRepository.findByProductId(product.getId()).orElse(null);
        int available = inventory != null ? inventory.getAvailableCylinder() : 0;

        Cart cart = getOrCreateCart(userId);
        if (cart.getItems() == null) {
            cart.setItems(new ArrayList<>());
        }

        Optional<Cart.CartItem> existingItemOpt = cart.getItems().stream()
                .filter(i -> i.getProductId().equals(request.getProductId()) && i.getPurchaseType() == request.getPurchaseType())
                .findFirst();

        int targetQuantity = request.getQuantity();
        if (existingItemOpt.isPresent()) {
            targetQuantity += existingItemOpt.get().getQuantity();
        }

        if (available < targetQuantity) {
            throw new BusinessException("Sản phẩm chỉ còn " + available + " bình trong kho");
        }

        if (existingItemOpt.isPresent()) {
            existingItemOpt.get().setQuantity(targetQuantity);
            existingItemOpt.get().setUnitPrice(product.getGasPrice());
            existingItemOpt.get().setCylinderFee(product.getCylinderFee());
        } else {
            cart.getItems().add(Cart.CartItem.builder()
                    .productId(product.getId())
                    .productName(product.getName())
                    .purchaseType(request.getPurchaseType())
                    .quantity(request.getQuantity())
                    .unitPrice(product.getGasPrice())
                    .cylinderFee(product.getCylinderFee())
                    .build());
        }

        cart.setUpdatedAt(LocalDateTime.now());
        Cart saved = cartRepository.save(cart);
        return buildCartResponse(saved);
    }

    @Override
    public CartResponse updateCartItem(String userId, UpdateCartItemRequest request) {
        Cart cart = getOrCreateCart(userId);
        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            return buildCartResponse(cart);
        }

        if (request.getQuantity() <= 0) {
            return removeCartItem(userId, request.getProductId(), request.getPurchaseType());
        }

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm không tồn tại"));

        Inventory inventory = inventoryRepository.findByProductId(product.getId()).orElse(null);
        int available = inventory != null ? inventory.getAvailableCylinder() : 0;
        if (available < request.getQuantity()) {
            throw new BusinessException("Kho chỉ còn " + available + " bình khả dụng");
        }

        for (Cart.CartItem item : cart.getItems()) {
            if (item.getProductId().equals(request.getProductId()) && item.getPurchaseType() == request.getPurchaseType()) {
                item.setQuantity(request.getQuantity());
                item.setUnitPrice(product.getGasPrice());
                item.setCylinderFee(product.getCylinderFee());
                break;
            }
        }

        cart.setUpdatedAt(LocalDateTime.now());
        Cart saved = cartRepository.save(cart);
        return buildCartResponse(saved);
    }

    @Override
    public CartResponse removeCartItem(String userId, String productId, PurchaseType purchaseType) {
        Cart cart = getOrCreateCart(userId);
        if (cart.getItems() != null) {
            cart.getItems().removeIf(i -> i.getProductId().equals(productId) && i.getPurchaseType() == purchaseType);
            cart.setUpdatedAt(LocalDateTime.now());
            cart = cartRepository.save(cart);
        }
        return buildCartResponse(cart);
    }

    @Override
    public void clearCart(String userId) {
        Cart cart = getOrCreateCart(userId);
        cart.setItems(new ArrayList<>());
        cart.setVoucherCode(null);
        cart.setVoucherId(null);
        cart.setUpdatedAt(LocalDateTime.now());
        cartRepository.save(cart);
    }

    @Override
    public CartResponse applyVoucher(String userId, String voucherCode) {
        Cart cart = getOrCreateCart(userId);
        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new BusinessException("Giỏ hàng của bạn đang trống");
        }

        // Kiểm tra hợp lệ bằng cách tính discount tạm
        double subtotal = calculateRawSubtotal(cart);
        double discount = voucherService.calculateDiscount(voucherCode, subtotal);
        if (discount <= 0) {
            throw new BusinessException("Voucher không hợp lệ hoặc đơn hàng chưa đạt giá trị tối thiểu");
        }

        cart.setVoucherCode(voucherCode.toUpperCase().trim());
        cart.setUpdatedAt(LocalDateTime.now());
        Cart saved = cartRepository.save(cart);
        return buildCartResponse(saved);
    }

    @Override
    public CartResponse removeVoucher(String userId) {
        Cart cart = getOrCreateCart(userId);
        cart.setVoucherCode(null);
        cart.setVoucherId(null);
        cart.setUpdatedAt(LocalDateTime.now());
        Cart saved = cartRepository.save(cart);
        return buildCartResponse(saved);
    }

    // ─────────────── HELPERS ───────────────

    private Cart getOrCreateCart(String userId) {
        return cartRepository.findByUserId(userId)
                .orElseGet(() -> cartRepository.save(Cart.builder()
                        .userId(userId)
                        .items(new ArrayList<>())
                        .updatedAt(LocalDateTime.now())
                        .build()));
    }

    private double calculateRawSubtotal(Cart cart) {
        if (cart.getItems() == null) return 0.0;
        double sum = 0.0;
        for (Cart.CartItem item : cart.getItems()) {
            Product p = productRepository.findById(item.getProductId()).orElse(null);
            if (p != null) {
                double gasPrice = p.getGasPrice() != null ? p.getGasPrice() : 0.0;
                double cylinderFee = (item.getPurchaseType() == PurchaseType.NEW_CYLINDER && p.getCylinderFee() != null)
                        ? p.getCylinderFee() : 0.0;
                sum += (gasPrice + cylinderFee) * item.getQuantity();
            }
        }
        return sum;
    }

    private CartResponse buildCartResponse(Cart cart) {
        List<CartResponse.CartItemResponse> itemResponses = new ArrayList<>();
        double gasSubtotal = 0.0;
        double cylinderFeeTotal = 0.0;

        if (cart.getItems() != null) {
            for (Cart.CartItem item : cart.getItems()) {
                Product product = productRepository.findById(item.getProductId()).orElse(null);
                if (product == null) continue;

                String brandName = null;
                if (product.getBrandId() != null) {
                    brandName = brandRepository.findById(product.getBrandId())
                            .map(Brand::getName).orElse(null);
                }

                Inventory inventory = inventoryRepository.findByProductId(product.getId()).orElse(null);
                int available = inventory != null ? inventory.getAvailableCylinder() : 0;
                boolean inStock = available >= item.getQuantity();

                double unitPrice = product.getGasPrice() != null ? product.getGasPrice() : 0.0;
                double cylinderFee = product.getCylinderFee() != null ? product.getCylinderFee() : 0.0;

                double itemGasSub = unitPrice * item.getQuantity();
                double itemCylSub = (item.getPurchaseType() == PurchaseType.NEW_CYLINDER)
                        ? cylinderFee * item.getQuantity() : 0.0;
                double itemTot = itemGasSub + itemCylSub;

                gasSubtotal += itemGasSub;
                cylinderFeeTotal += itemCylSub;

                String image = (product.getImages() != null && !product.getImages().isEmpty())
                        ? product.getImages().get(0) : null;

                itemResponses.add(CartResponse.CartItemResponse.builder()
                        .productId(product.getId())
                        .productName(product.getName())
                        .productImage(image)
                        .brandName(brandName)
                        .weightKg(product.getWeightKg())
                        .purchaseType(item.getPurchaseType())
                        .quantity(item.getQuantity())
                        .unitPrice(unitPrice)
                        .cylinderFee(cylinderFee)
                        .itemGasSubtotal(itemGasSub)
                        .itemCylinderSubtotal(itemCylSub)
                        .itemTotal(itemTot)
                        .inStock(inStock)
                        .availableStock(available)
                        .build());
            }
        }

        double subtotal = gasSubtotal + cylinderFeeTotal;

        // Store settings
        StoreSetting settings = storeSettingService.getSettings();
        double defaultDeliveryFee = settings.getDefaultDeliveryFee() != null ? settings.getDefaultDeliveryFee() : 30000.0;
        double freeThreshold = settings.getFreeShippingThreshold() != null ? settings.getFreeShippingThreshold() : 1000000.0;

        boolean isFreeShipping = subtotal >= freeThreshold && subtotal > 0;
        double deliveryFee = (subtotal > 0 && !isFreeShipping) ? defaultDeliveryFee : 0.0;

        // Discount
        double discountAmount = 0.0;
        if (cart.getVoucherCode() != null && subtotal > 0) {
            try {
                discountAmount = voucherService.calculateDiscount(cart.getVoucherCode(), subtotal);
            } catch (Exception e) {
                // Voucher không còn hợp lệ, xóa voucher khỏi cart
                cart.setVoucherCode(null);
                cartRepository.save(cart);
                discountAmount = 0.0;
            }
        }

        double finalAmount = Math.max(0.0, subtotal + deliveryFee - discountAmount);

        return CartResponse.builder()
                .id(cart.getId())
                .userId(cart.getUserId())
                .items(itemResponses)
                .voucherCode(cart.getVoucherCode())
                .gasSubtotal(gasSubtotal)
                .cylinderFeeTotal(cylinderFeeTotal)
                .subtotal(subtotal)
                .discountAmount(discountAmount)
                .deliveryFee(deliveryFee)
                .finalAmount(finalAmount)
                .isFreeShipping(isFreeShipping)
                .freeShippingThreshold(freeThreshold)
                .build();
    }
}
