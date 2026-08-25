package com.gasmanagement.controller;

import com.gasmanagement.dto.response.ApiResponse;
import com.gasmanagement.model.Inventory;
import com.gasmanagement.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/manager/inventory")
@RequiredArgsConstructor
@PreAuthorize("hasRole('MANAGER')")
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Inventory>>> getAllInventory() {
        // Có thể bổ sung get all trong InventoryService, tạm gọi qua chức năng hiện có hoặc cần implement thêm.
        // Tôi sẽ bổ sung thêm một hàm getAllInventories() trong InventoryService/Impl
        return ResponseEntity.ok(ApiResponse.ok(inventoryService.getAllInventories()));
    }
    
    @GetMapping("/{productId}")
    public ResponseEntity<ApiResponse<Inventory>> getInventoryByProduct(@PathVariable String productId) {
        return ResponseEntity.ok(ApiResponse.ok(inventoryService.getInventoryByProductId(productId)));
    }
    
    @PostMapping("/{productId}/import")
    public ResponseEntity<ApiResponse<String>> importStock(
            @PathVariable String productId,
            @RequestParam int quantity,
            @RequestParam String note,
            @org.springframework.security.core.annotation.AuthenticationPrincipal com.gasmanagement.security.UserDetailsImpl userDetails) {
        
        inventoryService.importStock(productId, quantity, note, userDetails.getId());
        return ResponseEntity.ok(ApiResponse.ok("Đã nhập " + quantity + " bình gas mới cho sản phẩm " + productId));
    }
}
