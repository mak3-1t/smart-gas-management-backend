package com.gasmanagement.service;

/**
 * Service quản lý tồn kho.
 * Mọi thay đổi số lượng đều phải ghi InventoryTransaction log.
 */
public interface InventoryService {

    /**
     * Xử lý khi giao hàng thành công theo kiểu EXCHANGE.
     * <ul>
     *   <li>fullCylinder -quantity (bình đầy đã giao)</li>
     *   <li>emptyCylinder +quantity (vỏ bình thu hồi từ khách)</li>
     *   <li>Ghi 2 InventoryTransaction: SALE và RETURN_EMPTY</li>
     * </ul>
     *
     * @param productId ID sản phẩm
     * @param quantity  Số lượng giao
     * @param orderId   Đơn hàng liên quan
     * @param staffId   Staff thực hiện
     */
    void processExchangeDelivery(String productId, int quantity, String orderId, String staffId);

    /**
     * Xử lý khi giao hàng thành công theo kiểu NEW_CYLINDER (mua mới, không thu vỏ).
     * <ul>
     *   <li>fullCylinder -quantity</li>
     *   <li>Ghi InventoryTransaction: SALE</li>
     * </ul>
     *
     * @param productId ID sản phẩm
     * @param quantity  Số lượng giao
     * @param orderId   Đơn hàng liên quan
     * @param staffId   Staff thực hiện
     */
    void processSaleDelivery(String productId, int quantity, String orderId, String staffId);

    /**
     * Reserve tồn kho khi Manager APPROVED một đơn hàng.
     * reservedCylinder +quantity
     *
     * @param productId ID sản phẩm
     * @param quantity  Số lượng cần giữ
     * @param orderId   Đơn hàng liên quan
     * @param managerId Manager thực hiện
     */
    void reserveStock(String productId, int quantity, String orderId, String managerId);

    /**
     * Giải phóng tồn kho đã reserve khi Order bị cancel sau khi APPROVED.
     * reservedCylinder -quantity
     *
     * @param productId ID sản phẩm
     * @param quantity  Số lượng cần giải phóng
     * @param orderId   Đơn hàng liên quan
     * @param actorId   Người thực hiện
     */
    void releaseReservation(String productId, int quantity, String orderId, String actorId);

    /**
     * Nhập kho (Manager import thêm hàng).
     *
     * @param productId ID sản phẩm
     * @param quantity  Số lượng nhập
     * @param note      Ghi chú
     * @param managerId Manager thực hiện
     */
    void importStock(String productId, int quantity, String note, String managerId);
}
