package com.gasmanagement.service;

import com.gasmanagement.dto.request.DeliveryActionRequest;
import com.gasmanagement.dto.request.DeliveryAssignRequest;
import com.gasmanagement.dto.response.DeliveryResponse;

import java.util.List;

/**
 * Service quản lý toàn bộ luồng Delivery:
 * Manager assign → Staff accept/reject → Start → Deliver → Complete/Fail
 */
public interface DeliveryService {

    // ─── MANAGER ───

    /**
     * Manager phân công Staff giao đơn.
     * Chỉ assign khi: ApprovalStatus=APPROVED + Staff AVAILABLE
     * COD: ORDER_PLACED, PaymentStatus=PENDING → vẫn được giao
     * Prepaid: OrderStatus=PAID
     */
    DeliveryResponse assignDelivery(DeliveryAssignRequest request);

    /** Danh sách delivery đang chờ assign (WAITING) */
    List<DeliveryResponse> getWaitingDeliveries();

    /** Danh sách toàn bộ delivery cho Manager */
    List<DeliveryResponse> getAllDeliveries();

    /** Chi tiết 1 delivery */
    DeliveryResponse getDeliveryById(String deliveryId);

    /** Delivery theo orderId */
    DeliveryResponse getDeliveryByOrderId(String orderId);

    // ─── STAFF ───

    /**
     * Staff CHẤP NHẬN đơn giao.
     * DeliveryStatus → ACCEPTED, StaffStatus → BUSY
     * Race condition: đảm bảo chỉ 1 staff accept thành công.
     */
    DeliveryResponse acceptDelivery(String deliveryId, DeliveryActionRequest request);

    /**
     * Staff TỪ CHỐI đơn giao.
     * Staff vẫn AVAILABLE, Delivery về WAITING chờ assign lại.
     */
    DeliveryResponse rejectDelivery(String deliveryId, DeliveryActionRequest request);

    /**
     * Staff BẮT ĐẦU đi giao.
     * DeliveryStatus → DELIVERING
     */
    DeliveryResponse startDelivery(String deliveryId, DeliveryActionRequest request);

    /**
     * Staff XÁC NHẬN giao thành công.
     * Kiểm tra:
     *   - PaymentStatus == PAID
     *   - Nếu EXCHANGE: CylinderExchangeStatus == COMPLETED
     * Sau đó:
     *   - DeliveryStatus → DELIVERED
     *   - OrderStatus → DELIVERED
     *   - StaffStatus → AVAILABLE (nếu hết đơn trong batch)
     *   - Cập nhật stats: successDeliveries++
     *   - Cập nhật Inventory (processExchangeDelivery / processSaleDelivery)
     */
    DeliveryResponse completeDelivery(String deliveryId, DeliveryActionRequest request);

    /**
     * Staff BÁO THẤT BẠI giao hàng.
     * DeliveryStatus → FAILED
     * StaffStatus → AVAILABLE
     * Cập nhật stats: failedDeliveries++
     * Manager sẽ quyết định reassign / cancel order sau.
     */
    DeliveryResponse failDelivery(String deliveryId, DeliveryActionRequest request);

    /**
     * Xử lý Cylinder Exchange khi giao hàng.
     * Staff xác nhận tình trạng vỏ bình thu hồi.
     * Nếu khách EXCHANGE nhưng không có vỏ → tính adjustment fee.
     */
    DeliveryResponse processCylinderExchange(String deliveryId, DeliveryActionRequest request);

    /** Danh sách delivery của 1 Staff */
    List<DeliveryResponse> getDeliveriesByStaff(String staffId);
}
