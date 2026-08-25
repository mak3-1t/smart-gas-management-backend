package com.gasmanagement.service.impl;

import com.gasmanagement.exception.ResourceNotFoundException;
import com.gasmanagement.model.Inventory;
import com.gasmanagement.model.InventoryTransaction;
import com.gasmanagement.model.enums.InventoryTransactionType;
import com.gasmanagement.repository.InventoryRepository;
import com.gasmanagement.repository.InventoryTransactionRepository;
import com.gasmanagement.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Xử lý tất cả nghiệp vụ tồn kho.
 * Mọi thay đổi số lượng đều kèm theo InventoryTransaction log.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository inventoryRepository;
    private final InventoryTransactionRepository transactionRepository;

    // ─────────────── DELIVERY FLOWS ───────────────

    @Override
    public void processExchangeDelivery(String productId, int quantity, String orderId, String staffId) {
        Inventory inv = findInventoryOrThrow(productId);

        // Xuất bình đầy
        inv.setFullCylinder(inv.getFullCylinder() - quantity);
        // Thu hồi vỏ rỗng
        inv.setEmptyCylinder(inv.getEmptyCylinder() + quantity);
        // Giảm reserved (đã reserve khi Manager approve)
        int newReserved = Math.max(0, inv.getReservedCylinder() - quantity);
        inv.setReservedCylinder(newReserved);

        inventoryRepository.save(inv);

        // Ghi log xuất bình đầy
        saveTransaction(InventoryTransaction.builder()
                .productId(productId)
                .orderId(orderId)
                .type(InventoryTransactionType.SALE)
                .quantityChange(-quantity)
                .cylinderType("FULL")
                .note("Giao hàng EXCHANGE – xuất bình đầy")
                .createdBy(staffId)
                .createdAt(LocalDateTime.now())
                .build());

        // Ghi log nhập vỏ rỗng
        saveTransaction(InventoryTransaction.builder()
                .productId(productId)
                .orderId(orderId)
                .type(InventoryTransactionType.RETURN_EMPTY)
                .quantityChange(+quantity)
                .cylinderType("EMPTY")
                .note("Giao hàng EXCHANGE – thu hồi vỏ rỗng từ khách")
                .createdBy(staffId)
                .createdAt(LocalDateTime.now())
                .build());

        log.info("EXCHANGE delivery processed: product={}, qty={}, order={}", productId, quantity, orderId);
    }

    @Override
    public void processSaleDelivery(String productId, int quantity, String orderId, String staffId) {
        Inventory inv = findInventoryOrThrow(productId);

        // Xuất bình đầy
        inv.setFullCylinder(inv.getFullCylinder() - quantity);
        // Giảm reserved
        int newReserved = Math.max(0, inv.getReservedCylinder() - quantity);
        inv.setReservedCylinder(newReserved);

        inventoryRepository.save(inv);

        saveTransaction(InventoryTransaction.builder()
                .productId(productId)
                .orderId(orderId)
                .type(InventoryTransactionType.SALE)
                .quantityChange(-quantity)
                .cylinderType("FULL")
                .note("Giao hàng NEW_CYLINDER – xuất bình đầy")
                .createdBy(staffId)
                .createdAt(LocalDateTime.now())
                .build());

        log.info("SALE delivery processed: product={}, qty={}, order={}", productId, quantity, orderId);
    }

    // ─────────────── RESERVATION ───────────────

    @Override
    public void reserveStock(String productId, int quantity, String orderId, String managerId) {
        Inventory inv = findInventoryOrThrow(productId);

        if (inv.getAvailableCylinder() < quantity) {
            throw new IllegalStateException(
                    "Không đủ tồn kho để reserve. Available: " + inv.getAvailableCylinder()
                    + ", Requested: " + quantity);
        }

        inv.setReservedCylinder(inv.getReservedCylinder() + quantity);
        inventoryRepository.save(inv);

        saveTransaction(InventoryTransaction.builder()
                .productId(productId)
                .orderId(orderId)
                .type(InventoryTransactionType.RESERVE)
                .quantityChange(+quantity)
                .cylinderType("RESERVED")
                .note("Reserve tồn kho khi Manager APPROVE đơn")
                .createdBy(managerId)
                .createdAt(LocalDateTime.now())
                .build());

        log.info("Stock reserved: product={}, qty={}, order={}", productId, quantity, orderId);
    }

    @Override
    public void releaseReservation(String productId, int quantity, String orderId, String actorId) {
        Inventory inv = findInventoryOrThrow(productId);

        int newReserved = Math.max(0, inv.getReservedCylinder() - quantity);
        inv.setReservedCylinder(newReserved);
        inventoryRepository.save(inv);

        saveTransaction(InventoryTransaction.builder()
                .productId(productId)
                .orderId(orderId)
                .type(InventoryTransactionType.RELEASE_RESERVATION)
                .quantityChange(-quantity)
                .cylinderType("RESERVED")
                .note("Giải phóng reserve khi Order bị cancel")
                .createdBy(actorId)
                .createdAt(LocalDateTime.now())
                .build());

        log.info("Reservation released: product={}, qty={}, order={}", productId, quantity, orderId);
    }

    // ─────────────── IMPORT ───────────────

    @Override
    public void importStock(String productId, int quantity, String note, String managerId) {
        Inventory inv = inventoryRepository.findByProductId(productId)
                .orElseGet(() -> Inventory.builder()
                        .productId(productId)
                        .build());

        inv.setFullCylinder(inv.getFullCylinder() + quantity);
        inventoryRepository.save(inv);

        saveTransaction(InventoryTransaction.builder()
                .productId(productId)
                .type(InventoryTransactionType.IMPORT)
                .quantityChange(+quantity)
                .cylinderType("FULL")
                .note(note != null ? note : "Nhập kho")
                .createdBy(managerId)
                .createdAt(LocalDateTime.now())
                .build());

        log.info("Stock imported: product={}, qty={}", productId, quantity);
    }

    // ─────────────── QUERY ───────────────

    @Override
    public java.util.List<Inventory> getAllInventories() {
        return inventoryRepository.findAll();
    }

    @Override
    public Inventory getInventoryByProductId(String productId) {
        return findInventoryOrThrow(productId);
    }

    // ─────────────── HELPERS ───────────────

    private Inventory findInventoryOrThrow(String productId) {
        return inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy inventory cho productId: " + productId));
    }

    private void saveTransaction(InventoryTransaction tx) {
        try {
            transactionRepository.save(tx);
        } catch (Exception e) {
            log.error("Lỗi ghi InventoryTransaction: {}", e.getMessage());
        }
    }
}
