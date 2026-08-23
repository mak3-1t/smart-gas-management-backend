package com.gasmanagement.controller;

import com.gasmanagement.dto.request.InventoryAdjustRequest;
import com.gasmanagement.dto.request.InventoryImportRequest;
import com.gasmanagement.dto.response.ApiResponse;
import com.gasmanagement.dto.response.InventoryResponse;
import com.gasmanagement.dto.response.InventoryTransactionResponse;
import com.gasmanagement.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    /**
     * [MANAGER] Xem tồn kho của 1 sản phẩm
     * GET /api/v1/inventory/product/{productId}
     */
    @GetMapping("/product/{productId}")
    public ResponseEntity<ApiResponse<InventoryResponse>> getByProductId(
            @PathVariable String productId) {
        return ResponseEntity.ok(
                ApiResponse.success(inventoryService.getInventoryByProductId(productId)));
    }

    /**
     * [MANAGER] Xem toàn bộ tồn kho
     * GET /api/v1/inventory
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<InventoryResponse>>> getAllInventories() {
        return ResponseEntity.ok(ApiResponse.success(inventoryService.getAllInventories()));
    }

    /**
     * [MANAGER] Xem sản phẩm sắp hết hàng
     * GET /api/v1/inventory/low-stock?threshold=5
     */
    @GetMapping("/low-stock")
    public ResponseEntity<ApiResponse<List<InventoryResponse>>> getLowStock(
            @RequestParam(defaultValue = "5") int threshold) {
        return ResponseEntity.ok(
                ApiResponse.success(inventoryService.getLowStockInventories(threshold)));
    }

    /**
     * [MANAGER] Nhập bình đầy vào kho
     * POST /api/v1/inventory/import
     *
     * Body:
     * {
     *   "productId": "...",
     *   "quantity": 20,
     *   "note": "Nhập từ nhà cung cấp",
     *   "createdBy": "managerId"
     * }
     */
    @PostMapping("/import")
    public ResponseEntity<ApiResponse<InventoryResponse>> importCylinders(
            @Valid @RequestBody InventoryImportRequest request) {
        InventoryResponse response = inventoryService.importCylinders(request);
        return ResponseEntity.ok(ApiResponse.success("Nhập kho thành công", response));
    }

    /**
     * [MANAGER] Điều chỉnh tồn kho (ADJUSTMENT / DAMAGED)
     * POST /api/v1/inventory/adjust
     *
     * Body:
     * {
     *   "productId": "...",
     *   "cylinderType": "FULL | EMPTY | DAMAGED",
     *   "quantityChange": -2,
     *   "note": "Bình hỏng do vận chuyển",
     *   "createdBy": "managerId"
     * }
     */
    @PostMapping("/adjust")
    public ResponseEntity<ApiResponse<InventoryResponse>> adjustInventory(
            @Valid @RequestBody InventoryAdjustRequest request) {
        InventoryResponse response = inventoryService.adjustInventory(request);
        return ResponseEntity.ok(ApiResponse.success("Điều chỉnh tồn kho thành công", response));
    }

    /**
     * [MANAGER] Lịch sử giao dịch kho theo sản phẩm
     * GET /api/v1/inventory/product/{productId}/transactions?page=0&size=20
     */
    @GetMapping("/product/{productId}/transactions")
    public ResponseEntity<ApiResponse<Page<InventoryTransactionResponse>>> getTransactions(
            @PathVariable String productId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<InventoryTransactionResponse> result =
                inventoryService.getTransactionsByProduct(productId, pageable);
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
