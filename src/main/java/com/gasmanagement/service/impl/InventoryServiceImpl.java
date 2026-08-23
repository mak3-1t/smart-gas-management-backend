package com.gasmanagement.service.impl;

import com.gasmanagement.dto.request.InventoryAdjustRequest;
import com.gasmanagement.dto.request.InventoryImportRequest;
import com.gasmanagement.dto.response.InventoryResponse;
import com.gasmanagement.dto.response.InventoryTransactionResponse;
import com.gasmanagement.exception.ResourceNotFoundException;
import com.gasmanagement.model.Inventory;
import com.gasmanagement.model.InventoryTransaction;
import com.gasmanagement.model.Product;
import com.gasmanagement.model.enums.InventoryTransactionType;
import com.gasmanagement.repository.InventoryRepository;
import com.gasmanagement.repository.InventoryTransactionRepository;
import com.gasmanagement.repository.ProductRepository;
import com.gasmanagement.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository inventoryRepository;
    private final InventoryTransactionRepository transactionRepository;
    private final ProductRepository productRepository;

    // ─────────────── READ ───────────────

    @Override
    public InventoryResponse getInventoryByProductId(String productId) {
        ensureProductExists(productId);
        Inventory inventory = getOrCreateInventory(productId);
        String productName = getProductName(productId);
        return mapToResponse(inventory, productName);
    }

    @Override
    public List<InventoryResponse> getAllInventories() {
        return inventoryRepository.findAll().stream()
                .map(inv -> {
                    String name = getProductName(inv.getProductId());
                    return mapToResponse(inv, name);
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<InventoryResponse> getLowStockInventories(int threshold) {
        return inventoryRepository.findAll().stream()
                .filter(inv -> {
                    int available = inv.getFullCylinder() - inv.getReservedCylinder();
                    return available <= threshold;
                })
                .map(inv -> {
                    String name = getProductName(inv.getProductId());
                    return mapToResponse(inv, name);
                })
                .collect(Collectors.toList());
    }

    @Override
    public Page<InventoryTransactionResponse> getTransactionsByProduct(String productId, Pageable pageable) {
        ensureProductExists(productId);
        String productName = getProductName(productId);
        return transactionRepository
                .findByProductIdOrderByCreatedAtDesc(productId, pageable)
                .map(tx -> mapTxToResponse(tx, productName));
    }

    // ─────────────── WRITE ───────────────

    @Override
    public InventoryResponse importCylinders(InventoryImportRequest request) {
        ensureProductExists(request.getProductId());
        Inventory inventory = getOrCreateInventory(request.getProductId());

        inventory.setFullCylinder(inventory.getFullCylinder() + request.getQuantity());
        inventoryRepository.save(inventory);

        saveTransaction(InventoryTransaction.builder()
                .productId(request.getProductId())
                .type(InventoryTransactionType.IMPORT)
                .quantityChange(request.getQuantity())
                .cylinderType("FULL")
                .note(request.getNote() != null ? request.getNote() : "Nhập kho")
                .createdBy(request.getCreatedBy())
                .createdAt(LocalDateTime.now())
                .build());

        String productName = getProductName(request.getProductId());
        return mapToResponse(inventory, productName);
    }

    @Override
    public InventoryResponse adjustInventory(InventoryAdjustRequest request) {
        ensureProductExists(request.getProductId());
        Inventory inventory = getOrCreateInventory(request.getProductId());

        switch (request.getCylinderType().toUpperCase()) {
            case "FULL"    -> inventory.setFullCylinder(
                    Math.max(0, inventory.getFullCylinder() + request.getQuantityChange()));
            case "EMPTY"   -> inventory.setEmptyCylinder(
                    Math.max(0, inventory.getEmptyCylinder() + request.getQuantityChange()));
            case "DAMAGED" -> inventory.setDamagedCylinder(
                    Math.max(0, inventory.getDamagedCylinder() + request.getQuantityChange()));
            default -> throw new IllegalArgumentException(
                    "cylinderType không hợp lệ: " + request.getCylinderType() +
                    ". Hợp lệ: FULL | EMPTY | DAMAGED");
        }
        inventoryRepository.save(inventory);

        saveTransaction(InventoryTransaction.builder()
                .productId(request.getProductId())
                .type(InventoryTransactionType.ADJUSTMENT)
                .quantityChange(request.getQuantityChange())
                .cylinderType(request.getCylinderType().toUpperCase())
                .note(request.getNote())
                .createdBy(request.getCreatedBy())
                .createdAt(LocalDateTime.now())
                .build());

        String productName = getProductName(request.getProductId());
        return mapToResponse(inventory, productName);
    }

    // ─────────────── BUSINESS OPERATIONS ───────────────

    @Override
    public void reserveStock(String productId, int quantity, String orderId, String createdBy) {
        Inventory inventory = getOrCreateInventory(productId);
        int available = inventory.getFullCylinder() - inventory.getReservedCylinder();
        if (available < quantity) {
            throw new IllegalStateException(
                    "Không đủ hàng để reserve. Khả dụng: " + available + ", Cần: " + quantity);
        }
        inventory.setReservedCylinder(inventory.getReservedCylinder() + quantity);
        inventoryRepository.save(inventory);

        saveTransaction(InventoryTransaction.builder()
                .productId(productId)
                .orderId(orderId)
                .type(InventoryTransactionType.RESERVE)
                .quantityChange(-quantity)
                .cylinderType("RESERVED")
                .note("Reserve cho đơn hàng " + orderId)
                .createdBy(createdBy)
                .createdAt(LocalDateTime.now())
                .build());
    }

    @Override
    public void releaseReservation(String productId, int quantity, String orderId, String createdBy) {
        Inventory inventory = getOrCreateInventory(productId);
        inventory.setReservedCylinder(Math.max(0, inventory.getReservedCylinder() - quantity));
        inventoryRepository.save(inventory);

        saveTransaction(InventoryTransaction.builder()
                .productId(productId)
                .orderId(orderId)
                .type(InventoryTransactionType.RELEASE_RESERVATION)
                .quantityChange(quantity)
                .cylinderType("RESERVED")
                .note("Release reservation cho đơn hàng " + orderId)
                .createdBy(createdBy)
                .createdAt(LocalDateTime.now())
                .build());
    }

    @Override
    public void processExchangeDelivery(String productId, int quantity, String orderId, String createdBy) {
        // Exchange: FULL -quantity, EMPTY +quantity, RESERVED -quantity
        Inventory inventory = getOrCreateInventory(productId);
        inventory.setFullCylinder(Math.max(0, inventory.getFullCylinder() - quantity));
        inventory.setReservedCylinder(Math.max(0, inventory.getReservedCylinder() - quantity));
        inventory.setEmptyCylinder(inventory.getEmptyCylinder() + quantity);
        inventoryRepository.save(inventory);

        // Log SALE (bình đầy ra)
        saveTransaction(InventoryTransaction.builder()
                .productId(productId).orderId(orderId)
                .type(InventoryTransactionType.SALE)
                .quantityChange(-quantity).cylinderType("FULL")
                .note("Bán (Exchange) - đơn " + orderId)
                .createdBy(createdBy).createdAt(LocalDateTime.now()).build());

        // Log RETURN_EMPTY (vỏ bình vào)
        saveTransaction(InventoryTransaction.builder()
                .productId(productId).orderId(orderId)
                .type(InventoryTransactionType.RETURN_EMPTY)
                .quantityChange(quantity).cylinderType("EMPTY")
                .note("Nhận vỏ bình rỗng - đơn " + orderId)
                .createdBy(createdBy).createdAt(LocalDateTime.now()).build());
    }

    @Override
    public void processSaleDelivery(String productId, int quantity, String orderId, String createdBy) {
        // NEW_CYLINDER: FULL -quantity, RESERVED -quantity
        Inventory inventory = getOrCreateInventory(productId);
        inventory.setFullCylinder(Math.max(0, inventory.getFullCylinder() - quantity));
        inventory.setReservedCylinder(Math.max(0, inventory.getReservedCylinder() - quantity));
        inventoryRepository.save(inventory);

        saveTransaction(InventoryTransaction.builder()
                .productId(productId).orderId(orderId)
                .type(InventoryTransactionType.SALE)
                .quantityChange(-quantity).cylinderType("FULL")
                .note("Bán (New Cylinder) - đơn " + orderId)
                .createdBy(createdBy).createdAt(LocalDateTime.now()).build());
    }

    // ─────────────── HELPER ───────────────

    private Inventory getOrCreateInventory(String productId) {
        return inventoryRepository.findByProductId(productId)
                .orElseGet(() -> {
                    Inventory newInv = Inventory.builder()
                            .productId(productId)
                            .fullCylinder(0)
                            .emptyCylinder(0)
                            .damagedCylinder(0)
                            .reservedCylinder(0)
                            .build();
                    return inventoryRepository.save(newInv);
                });
    }

    private void ensureProductExists(String productId) {
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("Không tìm thấy sản phẩm với id: " + productId);
        }
    }

    private String getProductName(String productId) {
        return productRepository.findById(productId)
                .map(Product::getName)
                .orElse("Unknown");
    }

    private void saveTransaction(InventoryTransaction tx) {
        transactionRepository.save(tx);
    }

    private InventoryResponse mapToResponse(Inventory inv, String productName) {
        int available = inv.getFullCylinder() - inv.getReservedCylinder();
        return InventoryResponse.builder()
                .id(inv.getId())
                .productId(inv.getProductId())
                .productName(productName)
                .fullCylinder(inv.getFullCylinder())
                .reservedCylinder(inv.getReservedCylinder())
                .availableCylinder(available)
                .emptyCylinder(inv.getEmptyCylinder())
                .damagedCylinder(inv.getDamagedCylinder())
                .lowStock(available <= 5)
                .build();
    }

    private InventoryTransactionResponse mapTxToResponse(InventoryTransaction tx, String productName) {
        return InventoryTransactionResponse.builder()
                .id(tx.getId())
                .productId(tx.getProductId())
                .productName(productName)
                .orderId(tx.getOrderId())
                .type(tx.getType())
                .quantityChange(tx.getQuantityChange())
                .cylinderType(tx.getCylinderType())
                .note(tx.getNote())
                .createdBy(tx.getCreatedBy())
                .createdAt(tx.getCreatedAt())
                .build();
    }
}
