package com.gasmanagement.service;

import com.gasmanagement.dto.request.AddToCartRequest;
import com.gasmanagement.dto.response.CartResponse;
import com.gasmanagement.model.*;
import com.gasmanagement.model.enums.ProductStatus;
import com.gasmanagement.model.enums.PurchaseType;
import com.gasmanagement.repository.BrandRepository;
import com.gasmanagement.repository.CartRepository;
import com.gasmanagement.repository.InventoryRepository;
import com.gasmanagement.repository.ProductRepository;
import com.gasmanagement.service.impl.CartServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private BrandRepository brandRepository;

    @Mock
    private VoucherService voucherService;

    @Mock
    private StoreSettingService storeSettingService;

    @InjectMocks
    private CartServiceImpl cartService;

    private Product gasProduct;
    private Inventory gasInventory;
    private StoreSetting defaultSettings;

    @BeforeEach
    void setUp() {
        gasProduct = Product.builder()
                .id("prod-1")
                .name("Bình Gas Petrolimex 12kg")
                .gasPrice(450000.0)
                .cylinderFee(500000.0)
                .weightKg(12.0)
                .status(ProductStatus.ACTIVE)
                .build();

        gasInventory = Inventory.builder()
                .productId("prod-1")
                .fullCylinder(50)
                .reservedCylinder(0)
                .build();

        defaultSettings = StoreSetting.builder()
                .defaultDeliveryFee(30000.0)
                .freeShippingThreshold(1000000.0)
                .build();

        lenient().when(storeSettingService.getSettings()).thenReturn(defaultSettings);
    }

    @Test
    @DisplayName("Nghiệp vụ EXCHANGE: Đổi vỏ cũ -> Không tính phí vỏ bình (CylinderFee = 0)")
    void testAddToCart_Exchange_NoCylinderFee() {
        String userId = "user-123";
        AddToCartRequest request = AddToCartRequest.builder()
                .productId("prod-1")
                .purchaseType(PurchaseType.EXCHANGE)
                .quantity(1)
                .build();

        when(productRepository.findById("prod-1")).thenReturn(Optional.of(gasProduct));
        when(inventoryRepository.findByProductId("prod-1")).thenReturn(Optional.of(gasInventory));
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CartResponse response = cartService.addToCart(userId, request);

        assertNotNull(response);
        assertEquals(450000.0, response.getGasSubtotal());
        assertEquals(0.0, response.getCylinderFeeTotal());
        assertEquals(450000.0, response.getSubtotal());
        assertEquals(30000.0, response.getDeliveryFee()); // Dưới 1 triệu -> tính phí ship
        assertEquals(480000.0, response.getFinalAmount());
    }

    @Test
    @DisplayName("Nghiệp vụ NEW_CYLINDER: Mua mới bình gas -> Tính thêm phí vỏ bình")
    void testAddToCart_NewCylinder_IncludesCylinderFee() {
        String userId = "user-123";
        AddToCartRequest request = AddToCartRequest.builder()
                .productId("prod-1")
                .purchaseType(PurchaseType.NEW_CYLINDER)
                .quantity(1)
                .build();

        when(productRepository.findById("prod-1")).thenReturn(Optional.of(gasProduct));
        when(inventoryRepository.findByProductId("prod-1")).thenReturn(Optional.of(gasInventory));
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CartResponse response = cartService.addToCart(userId, request);

        assertNotNull(response);
        assertEquals(450000.0, response.getGasSubtotal());
        assertEquals(500000.0, response.getCylinderFeeTotal());
        assertEquals(950000.0, response.getSubtotal());
        assertEquals(30000.0, response.getDeliveryFee());
        assertEquals(980000.0, response.getFinalAmount());
    }

    @Test
    @DisplayName("Nghiệp vụ Free Shipping: Đơn hàng trên ngưỡng Free Shipping (>= 1,000,000) -> Phí ship = 0")
    void testAddToCart_FreeShippingThreshold() {
        String userId = "user-123";
        // Mua 2 bình mới: (450k + 500k) * 2 = 1,900,000 >= 1,000,000
        AddToCartRequest request = AddToCartRequest.builder()
                .productId("prod-1")
                .purchaseType(PurchaseType.NEW_CYLINDER)
                .quantity(2)
                .build();

        when(productRepository.findById("prod-1")).thenReturn(Optional.of(gasProduct));
        when(inventoryRepository.findByProductId("prod-1")).thenReturn(Optional.of(gasInventory));
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CartResponse response = cartService.addToCart(userId, request);

        assertNotNull(response);
        assertEquals(1900000.0, response.getSubtotal());
        assertTrue(response.isFreeShipping());
        assertEquals(0.0, response.getDeliveryFee());
        assertEquals(1900000.0, response.getFinalAmount());
    }
}
