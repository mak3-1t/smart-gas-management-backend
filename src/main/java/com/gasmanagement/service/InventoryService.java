package com.gasmanagement.service;

import com.gasmanagement.dto.request.InventoryAdjustRequest;
import com.gasmanagement.dto.request.InventoryImportRequest;
import com.gasmanagement.dto.response.InventoryResponse;
import com.gasmanagement.dto.response.InventoryTransactionResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface InventoryService {

    /** Xem tồn kho theo sản phẩm */
    InventoryResponse getInventoryByProductId(String productId);

    /** Xem tất cả tồn kho */
    List<InventoryResponse> getAllInventories();

    /** Nhập bình đầy vào kho */
    InventoryResponse importCylinders(InventoryImportRequest request);

    /** Điều chỉnh số lượng (DAMAGED, ADJUSTMENT) */
    InventoryResponse adjustInventory(InventoryAdjustRequest request);

    /** Danh sách sản phẩm sắp hết hàng (available <= threshold) */
    List<InventoryResponse> getLowStockInventories(int threshold);

    /** Lịch sử giao dịch kho theo sản phẩm */
    Page<InventoryTransactionResponse> getTransactionsByProduct(String productId, Pageable pageable);

    /** Reserve stock khi Order được Approve */
    void reserveStock(String productId, int quantity, String orderId, String createdBy);

    /** Release reservation khi Order bị Cancel */
    void releaseReservation(String productId, int quantity, String orderId, String createdBy);

    /** Trừ stock và cộng empty khi giao Exchange thành công */
    void processExchangeDelivery(String productId, int quantity, String orderId, String createdBy);

    /** Trừ stock khi giao NEW_CYLINDER thành công */
    void processSaleDelivery(String productId, int quantity, String orderId, String createdBy);
}
