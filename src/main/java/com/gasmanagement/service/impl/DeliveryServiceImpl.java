package com.gasmanagement.service.impl;

import com.gasmanagement.dto.request.DeliveryActionRequest;
import com.gasmanagement.dto.request.DeliveryAssignRequest;
import com.gasmanagement.dto.response.DeliveryResponse;
import com.gasmanagement.exception.ResourceNotFoundException;
import com.gasmanagement.model.Delivery;
import com.gasmanagement.model.Order;
import com.gasmanagement.model.StaffProfile;
import com.gasmanagement.model.enums.*;
import com.gasmanagement.repository.DeliveryRepository;
import com.gasmanagement.repository.OrderRepository;
import com.gasmanagement.repository.StaffProfileRepository;
import com.gasmanagement.repository.UserRepository;
import com.gasmanagement.service.DeliveryService;
import com.gasmanagement.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeliveryServiceImpl implements DeliveryService {

    private final DeliveryRepository deliveryRepository;
    private final OrderRepository orderRepository;
    private final StaffProfileRepository staffProfileRepository;
    private final UserRepository userRepository;
    private final InventoryService inventoryService;

    // ─────────────── MANAGER ───────────────

    @Override
    public DeliveryResponse assignDelivery(DeliveryAssignRequest request) {
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy đơn hàng: " + request.getOrderId()));

        // Validate: Order phải được APPROVED trước khi assign
        if (order.getApprovalStatus() != ApprovalStatus.APPROVED) {
            throw new IllegalStateException(
                    "Đơn hàng chưa được Manager duyệt. ApprovalStatus hiện tại: "
                    + order.getApprovalStatus());
        }

        // COD: ORDER_PLACED + PENDING → vẫn được giao
        // Prepaid: phải PAID
        boolean isCod = order.getPaymentMethod() == PaymentMethod.COD;
        if (!isCod && order.getPaymentStatus() != PaymentStatus.PAID) {
            throw new IllegalStateException(
                    "Đơn thanh toán online chưa được thanh toán. PaymentStatus: "
                    + order.getPaymentStatus());
        }

        // Validate Staff tồn tại và AVAILABLE
        StaffProfile staff = staffProfileRepository.findByUserId(request.getStaffId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy staff: " + request.getStaffId()));

        if (staff.getStaffStatus() != StaffStatus.AVAILABLE) {
            throw new IllegalStateException(
                    "Staff không khả dụng. Trạng thái hiện tại: " + staff.getStaffStatus());
        }

        // Tạo Delivery
        Delivery delivery = Delivery.builder()
                .orderId(request.getOrderId())
                .staffId(request.getStaffId())
                .deliveryStatus(DeliveryStatus.WAITING)
                .batchId(request.getBatchId())
                .deliverySequence(request.getDeliverySequence())
                .assignedAt(LocalDateTime.now())
                .statusHistory(new ArrayList<>())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        addStatusHistory(delivery, DeliveryStatus.WAITING.name(), "SYSTEM",
                "Assigned to staff: " + request.getStaffId());

        Delivery saved = deliveryRepository.save(delivery);
        log.info("Delivery {} assigned to staff {} for order {}",
                saved.getId(), request.getStaffId(), request.getOrderId());

        return mapToResponse(saved);
    }

    @Override
    public List<DeliveryResponse> getWaitingDeliveries() {
        return deliveryRepository
                .findByDeliveryStatusOrderByCreatedAtAsc(DeliveryStatus.WAITING)
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    public List<DeliveryResponse> getAllDeliveries() {
        return deliveryRepository.findAll().stream()
                .map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    public DeliveryResponse getDeliveryById(String deliveryId) {
        return mapToResponse(findDeliveryOrThrow(deliveryId));
    }

    @Override
    public DeliveryResponse getDeliveryByOrderId(String orderId) {
        return deliveryRepository.findByOrderId(orderId)
                .map(this::mapToResponse)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy delivery cho orderId: " + orderId));
    }

    // ─────────────── STAFF ACTIONS ───────────────

    @Override
    public DeliveryResponse acceptDelivery(String deliveryId, DeliveryActionRequest request) {
        Delivery delivery = findDeliveryOrThrow(deliveryId);

        // Race condition check: chỉ WAITING mới được accept
        if (delivery.getDeliveryStatus() != DeliveryStatus.WAITING) {
            throw new IllegalStateException(
                    "Đơn giao này không thể nhận. Trạng thái: "
                    + delivery.getDeliveryStatus()
                    + ". Có thể đã được Staff khác nhận rồi.");
        }

        // Update Delivery
        delivery.setDeliveryStatus(DeliveryStatus.ACCEPTED);
        delivery.setAcceptedAt(LocalDateTime.now());
        delivery.setUpdatedAt(LocalDateTime.now());
        addStatusHistory(delivery, DeliveryStatus.ACCEPTED.name(),
                request.getStaffId(), "Staff đã nhận đơn");

        deliveryRepository.save(delivery);

        // Cập nhật Staff → BUSY + thêm vào activeDeliveryIds
        StaffProfile staff = staffProfileRepository.findByUserId(delivery.getStaffId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy staff profile: " + delivery.getStaffId()));

        staff.setStaffStatus(StaffStatus.BUSY);
        if (staff.getActiveDeliveryIds() == null) staff.setActiveDeliveryIds(new ArrayList<>());
        staff.getActiveDeliveryIds().add(deliveryId);
        staffProfileRepository.save(staff);

        log.info("Delivery {} ACCEPTED by staff {}", deliveryId, request.getStaffId());
        return mapToResponse(delivery);
    }

    @Override
    public DeliveryResponse rejectDelivery(String deliveryId, DeliveryActionRequest request) {
        Delivery delivery = findDeliveryOrThrow(deliveryId);

        if (delivery.getDeliveryStatus() != DeliveryStatus.WAITING) {
            throw new IllegalStateException("Chỉ có thể từ chối đơn đang WAITING");
        }

        // Staff vẫn AVAILABLE, xóa khỏi active list
        StaffProfile staff = staffProfileRepository.findByUserId(delivery.getStaffId()).orElse(null);
        if (staff != null) {
            staff.setStaffStatus(StaffStatus.AVAILABLE);
            if (staff.getActiveDeliveryIds() != null)
                staff.getActiveDeliveryIds().remove(deliveryId);
            staffProfileRepository.save(staff);
        }

        // Delivery về WAITING, xóa staffId để chờ Manager assign lại
        delivery.setStaffId(null);
        delivery.setUpdatedAt(LocalDateTime.now());
        addStatusHistory(delivery, "REJECTED_BY_STAFF",
                request.getStaffId(),
                "Staff từ chối: " + (request.getNote() != null ? request.getNote() : ""));

        deliveryRepository.save(delivery);
        log.info("Delivery {} REJECTED by staff {}", deliveryId, request.getStaffId());
        return mapToResponse(delivery);
    }

    @Override
    public DeliveryResponse startDelivery(String deliveryId, DeliveryActionRequest request) {
        Delivery delivery = findDeliveryOrThrow(deliveryId);

        if (delivery.getDeliveryStatus() != DeliveryStatus.ACCEPTED) {
            throw new IllegalStateException("Chỉ có thể bắt đầu giao khi đơn ở trạng thái ACCEPTED");
        }

        delivery.setDeliveryStatus(DeliveryStatus.DELIVERING);
        delivery.setStartedAt(LocalDateTime.now());
        delivery.setUpdatedAt(LocalDateTime.now());
        addStatusHistory(delivery, DeliveryStatus.DELIVERING.name(),
                request.getStaffId(), "Bắt đầu giao hàng");

        deliveryRepository.save(delivery);
        log.info("Delivery {} STARTED by staff {}", deliveryId, request.getStaffId());
        return mapToResponse(delivery);
    }

    @Override
    public DeliveryResponse completeDelivery(String deliveryId, DeliveryActionRequest request) {
        Delivery delivery = findDeliveryOrThrow(deliveryId);

        if (delivery.getDeliveryStatus() != DeliveryStatus.DELIVERING) {
            throw new IllegalStateException("Chỉ có thể hoàn thành khi đơn đang DELIVERING");
        }

        // Lấy order kiểm tra payment
        Order order = orderRepository.findById(delivery.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy đơn hàng: " + delivery.getOrderId()));

        // Kiểm tra đã thanh toán chưa
        if (order.getPaymentStatus() != PaymentStatus.PAID) {
            throw new IllegalStateException(
                    "Đơn chưa được thanh toán. PaymentStatus: " + order.getPaymentStatus()
                    + ". Vui lòng xác nhận thu tiền trước.");
        }

        // Kiểm tra Cylinder Exchange nếu cần
        boolean hasExchange = order.getItems() != null && order.getItems().stream()
                .anyMatch(item -> item.getPurchaseType() == PurchaseType.EXCHANGE);

        if (hasExchange) {
            boolean allExchangeCompleted = order.getItems().stream()
                    .filter(item -> item.getPurchaseType() == PurchaseType.EXCHANGE)
                    .allMatch(item -> item.getCylinderExchangeStatus() == CylinderExchangeStatus.COMPLETED);

            if (!allExchangeCompleted) {
                throw new IllegalStateException(
                        "Cần xác nhận thu hồi vỏ bình trước khi hoàn thành giao hàng.");
            }
        }

        // Cập nhật Inventory theo từng item
        if (order.getItems() != null) {
            for (Order.OrderItem item : order.getItems()) {
                try {
                    if (item.getPurchaseType() == PurchaseType.EXCHANGE) {
                        inventoryService.processExchangeDelivery(
                                item.getProductId(), item.getQuantity(),
                                order.getId(), request.getStaffId());
                    } else {
                        inventoryService.processSaleDelivery(
                                item.getProductId(), item.getQuantity(),
                                order.getId(), request.getStaffId());
                    }
                } catch (Exception e) {
                    log.error("Lỗi cập nhật inventory cho sản phẩm {}: {}",
                            item.getProductId(), e.getMessage());
                }
            }
        }

        // Cập nhật Delivery
        delivery.setDeliveryStatus(DeliveryStatus.DELIVERED);
        delivery.setDeliveredAt(LocalDateTime.now());
        delivery.setUpdatedAt(LocalDateTime.now());
        addStatusHistory(delivery, DeliveryStatus.DELIVERED.name(),
                request.getStaffId(), "Giao hàng thành công");
        deliveryRepository.save(delivery);

        // Cập nhật Order → DELIVERED
        order.setOrderStatus(OrderStatus.DELIVERED);
        order.setUpdatedAt(LocalDateTime.now());
        if (order.getStatusHistory() == null) order.setStatusHistory(new ArrayList<>());
        order.getStatusHistory().add(Order.StatusHistory.builder()
                .status(OrderStatus.DELIVERED.name())
                .changedBy(request.getStaffId())
                .note("Staff xác nhận giao thành công")
                .changedAt(LocalDateTime.now())
                .build());
        orderRepository.save(order);

        // Cập nhật Staff stats + xóa khỏi active list
        updateStaffAfterDelivery(delivery.getStaffId(), deliveryId, true);

        log.info("Delivery {} COMPLETED by staff {}", deliveryId, request.getStaffId());
        return mapToResponse(delivery);
    }

    @Override
    public DeliveryResponse failDelivery(String deliveryId, DeliveryActionRequest request) {
        Delivery delivery = findDeliveryOrThrow(deliveryId);

        if (delivery.getDeliveryStatus() == DeliveryStatus.DELIVERED ||
                delivery.getDeliveryStatus() == DeliveryStatus.FAILED) {
            throw new IllegalStateException("Đơn đã hoàn thành hoặc đã thất bại trước đó.");
        }

        if (request.getFailureReason() == null) {
            throw new IllegalArgumentException("Phải chọn lý do thất bại khi báo giao hàng thất bại");
        }

        delivery.setDeliveryStatus(DeliveryStatus.FAILED);
        delivery.setFailureReason(request.getFailureReason());
        delivery.setFailureNote(request.getNote());
        delivery.setUpdatedAt(LocalDateTime.now());
        addStatusHistory(delivery, DeliveryStatus.FAILED.name(),
                request.getStaffId(),
                "Thất bại: " + request.getFailureReason() + " - " + request.getNote());
        deliveryRepository.save(delivery);

        // Cập nhật Staff stats
        updateStaffAfterDelivery(delivery.getStaffId(), deliveryId, false);

        log.info("Delivery {} FAILED by staff {}, reason: {}",
                deliveryId, request.getStaffId(), request.getFailureReason());
        return mapToResponse(delivery);
    }

    @Override
    public DeliveryResponse processCylinderExchange(String deliveryId, DeliveryActionRequest request) {
        Delivery delivery = findDeliveryOrThrow(deliveryId);

        if (delivery.getDeliveryStatus() != DeliveryStatus.DELIVERING) {
            throw new IllegalStateException("Chỉ xử lý vỏ bình khi đang DELIVERING");
        }

        Order order = orderRepository.findById(delivery.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy đơn hàng: " + delivery.getOrderId()));

        // Cập nhật CylinderExchangeStatus + condition cho tất cả items EXCHANGE
        boolean hasNoEmptyCylinder = request.getFailureReason() == DeliveryFailureReason.NO_EMPTY_CYLINDER;

        if (order.getItems() != null) {
            order.getItems().stream()
                    .filter(item -> item.getPurchaseType() == PurchaseType.EXCHANGE)
                    .forEach(item -> {
                        item.setCylinderExchangeStatus(
                                hasNoEmptyCylinder
                                        ? CylinderExchangeStatus.NO_CYLINDER
                                        : CylinderExchangeStatus.COMPLETED);
                        item.setCylinderCondition(request.getCylinderCondition());
                    });
        }

        // Nếu khách không có vỏ bình → tính adjustment fee
        if (hasNoEmptyCylinder && request.getAdjustmentFee() != null) {
            order.setAdjustmentFee(request.getAdjustmentFee());
            order.setAdjustmentNote(request.getAdjustmentNote());
            // Tính lại finalAmount
            Double currentFinal = order.getFinalAmount() != null ? order.getFinalAmount() : 0.0;
            order.setFinalAmount(currentFinal + request.getAdjustmentFee());
        }

        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);

        addStatusHistory(delivery, "CYLINDER_EXCHANGE_PROCESSED",
                request.getStaffId(),
                hasNoEmptyCylinder ? "Khách không có vỏ bình, tính phí bổ sung" : "Thu hồi vỏ bình thành công");
        delivery.setUpdatedAt(LocalDateTime.now());
        deliveryRepository.save(delivery);

        log.info("Cylinder exchange processed for delivery {}", deliveryId);
        return mapToResponse(delivery);
    }

    @Override
    public List<DeliveryResponse> getDeliveriesByStaff(String staffId) {
        return deliveryRepository.findByStaffId(staffId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ─────────────── PRIVATE HELPERS ───────────────

    private Delivery findDeliveryOrThrow(String deliveryId) {
        return deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy delivery: " + deliveryId));
    }

    private void addStatusHistory(Delivery delivery, String status, String note, String changedBy) {
        if (delivery.getStatusHistory() == null) delivery.setStatusHistory(new ArrayList<>());
        delivery.getStatusHistory().add(
                Delivery.StatusHistory.builder()
                        .status(status)
                        .note(changedBy + ": " + note)
                        .changedAt(LocalDateTime.now())
                        .build()
        );
    }

    /**
     * Cập nhật StaffProfile sau khi delivery hoàn thành hoặc thất bại.
     */
    private void updateStaffAfterDelivery(String staffId, String deliveryId, boolean success) {
        staffProfileRepository.findByUserId(staffId).ifPresent(staff -> {
            staff.setTotalDeliveries(staff.getTotalDeliveries() + 1);
            if (success) staff.setSuccessDeliveries(staff.getSuccessDeliveries() + 1);
            else         staff.setFailedDeliveries(staff.getFailedDeliveries() + 1);

            // Xóa khỏi activeDeliveryIds
            if (staff.getActiveDeliveryIds() != null)
                staff.getActiveDeliveryIds().remove(deliveryId);

            // Nếu không còn đơn nào active → AVAILABLE
            if (staff.getActiveDeliveryIds() == null || staff.getActiveDeliveryIds().isEmpty()) {
                staff.setStaffStatus(StaffStatus.AVAILABLE);
            }

            staffProfileRepository.save(staff);
        });
    }

    private DeliveryResponse mapToResponse(Delivery delivery) {
        String staffName = null;
        if (delivery.getStaffId() != null) {
            staffName = userRepository.findById(delivery.getStaffId())
                    .map(u -> u.getUsername())
                    .orElse(null);
        }

        return DeliveryResponse.builder()
                .id(delivery.getId())
                .orderId(delivery.getOrderId())
                .staffId(delivery.getStaffId())
                .staffName(staffName)
                .deliveryStatus(delivery.getDeliveryStatus())
                .batchId(delivery.getBatchId())
                .deliverySequence(delivery.getDeliverySequence())
                .failureReason(delivery.getFailureReason())
                .failureNote(delivery.getFailureNote())
                .assignedAt(delivery.getAssignedAt())
                .acceptedAt(delivery.getAcceptedAt())
                .startedAt(delivery.getStartedAt())
                .deliveredAt(delivery.getDeliveredAt())
                .statusHistory(delivery.getStatusHistory())
                .createdAt(delivery.getCreatedAt())
                .updatedAt(delivery.getUpdatedAt())
                .build();
    }
}
