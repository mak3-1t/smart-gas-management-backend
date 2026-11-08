package com.gasmanagement.service.impl;

import com.gasmanagement.dto.request.DeliveryActionRequest;
import com.gasmanagement.dto.request.DeliveryAssignRequest;
import com.gasmanagement.dto.response.DeliveryResponse;
import com.gasmanagement.exception.BusinessException;
import com.gasmanagement.exception.ResourceNotFoundException;
import com.gasmanagement.model.CustomerProfile;
import com.gasmanagement.model.Delivery;
import com.gasmanagement.model.Order;
import com.gasmanagement.model.Payment;
import com.gasmanagement.model.StaffProfile;
import com.gasmanagement.model.User;
import com.gasmanagement.model.enums.*;
import com.gasmanagement.repository.CustomerProfileRepository;
import com.gasmanagement.repository.DeliveryRepository;
import com.gasmanagement.repository.OrderRepository;
import com.gasmanagement.repository.PaymentRepository;
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
    private final PaymentRepository paymentRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final StaffProfileRepository staffProfileRepository;
    private final UserRepository userRepository;
    private final InventoryService inventoryService;

    // ─────────────── MANAGER ───────────────

    @Override
    public DeliveryResponse assignDelivery(DeliveryAssignRequest request) {
        String rawOrderId = request.getOrderId() != null ? request.getOrderId().trim().replaceFirst("^#", "") : "";
        Order order = orderRepository.findById(rawOrderId)
                .orElseGet(() -> orderRepository.findByOrderCode(rawOrderId)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Không tìm thấy đơn hàng: " + request.getOrderId())));

        if (order.getOrderStatus() == OrderStatus.CANCELLED || order.getOrderStatus() == OrderStatus.DELIVERED) {
            throw new BusinessException("Không thể phân công đơn hàng ở trạng thái: " + order.getOrderStatus());
        }

        // Nếu đơn đang PENDING_APPROVAL mà Manager phân công luôn thì tự động APPROVE đơn
        if (order.getApprovalStatus() == ApprovalStatus.PENDING_APPROVAL) {
            order.setApprovalStatus(ApprovalStatus.APPROVED);
            order.setUpdatedAt(LocalDateTime.now());
            orderRepository.save(order);
        }

        if (order.getApprovalStatus() != ApprovalStatus.APPROVED) {
            throw new IllegalStateException(
                    "Đơn hàng chưa được Manager duyệt. ApprovalStatus hiện tại: "
                    + order.getApprovalStatus());
        }

        StaffProfile staff = staffProfileRepository.findByUserId(request.getStaffId())
                .orElseGet(() -> staffProfileRepository.findById(request.getStaffId())
                        .orElseGet(() -> userRepository.findByUsername(request.getStaffId())
                                .flatMap(u -> staffProfileRepository.findByUserId(u.getId()))
                                .orElseThrow(() -> new ResourceNotFoundException(
                                        "Không tìm thấy staff: " + request.getStaffId()))));

        String resolvedStaffUserId = staff.getUserId();

        // Kiểm tra xem đã có Delivery cho order này chưa
        Delivery delivery = deliveryRepository.findByOrderId(order.getId())
                .orElseGet(() -> Delivery.builder()
                        .orderId(order.getId())
                        .statusHistory(new ArrayList<>())
                        .createdAt(LocalDateTime.now())
                        .build());

        // Nếu trước đó đã gán cho staff khác thì gỡ khỏi activeDeliveryIds của staff cũ
        if (delivery.getId() != null && delivery.getStaffId() != null
                && !delivery.getStaffId().equals(resolvedStaffUserId)) {
            staffProfileRepository.findByUserId(delivery.getStaffId()).ifPresent(oldStaff -> {
                if (oldStaff.getActiveDeliveryIds() != null) {
                    oldStaff.getActiveDeliveryIds().remove(delivery.getId());
                    if (oldStaff.getActiveDeliveryIds().isEmpty() && oldStaff.getStaffStatus() == StaffStatus.BUSY) {
                        oldStaff.setStaffStatus(StaffStatus.AVAILABLE);
                    }
                    staffProfileRepository.save(oldStaff);
                }
            });
        }

        delivery.setStaffId(resolvedStaffUserId);
        delivery.setDeliveryStatus(DeliveryStatus.WAITING);
        delivery.setBatchId(request.getBatchId());
        delivery.setDeliverySequence(request.getDeliverySequence());
        delivery.setAssignedAt(LocalDateTime.now());
        delivery.setUpdatedAt(LocalDateTime.now());

        addStatusHistory(delivery, DeliveryStatus.WAITING.name(), "SYSTEM",
                "Assigned to staff: " + resolvedStaffUserId);

        Delivery saved = deliveryRepository.save(delivery);

        if (staff.getActiveDeliveryIds() == null) {
            staff.setActiveDeliveryIds(new ArrayList<>());
        }
        if (!staff.getActiveDeliveryIds().contains(saved.getId())) {
            staff.getActiveDeliveryIds().add(saved.getId());
            staffProfileRepository.save(staff);
        }

        log.info("Delivery {} assigned to staff {} for order {}",
                saved.getId(), resolvedStaffUserId, order.getId());

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

        if (delivery.getDeliveryStatus() != DeliveryStatus.WAITING) {
            throw new IllegalStateException(
                    "Đơn giao này không thể nhận. Trạng thái: "
                    + delivery.getDeliveryStatus()
                    + ". Có thể đã được Staff khác nhận rồi.");
        }

        delivery.setDeliveryStatus(DeliveryStatus.ACCEPTED);
        delivery.setAcceptedAt(LocalDateTime.now());
        delivery.setUpdatedAt(LocalDateTime.now());
        addStatusHistory(delivery, DeliveryStatus.ACCEPTED.name(),
                request.getStaffId(), "Staff đã nhận đơn");

        deliveryRepository.save(delivery);

        StaffProfile staff = staffProfileRepository.findByUserId(delivery.getStaffId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy staff profile: " + delivery.getStaffId()));

        staff.setStaffStatus(StaffStatus.BUSY);
        if (staff.getActiveDeliveryIds() == null) staff.setActiveDeliveryIds(new ArrayList<>());
        if (!staff.getActiveDeliveryIds().contains(delivery.getId())) {
            staff.getActiveDeliveryIds().add(delivery.getId());
        }
        staffProfileRepository.save(staff);

        log.info("Delivery {} ACCEPTED by staff {}", delivery.getId(), request.getStaffId());
        return mapToResponse(delivery);
    }

    @Override
    public DeliveryResponse rejectDelivery(String deliveryId, DeliveryActionRequest request) {
        Delivery delivery = findDeliveryOrThrow(deliveryId);

        if (delivery.getDeliveryStatus() != DeliveryStatus.WAITING) {
            throw new IllegalStateException("Chỉ có thể từ chối đơn đang WAITING");
        }

        StaffProfile staff = staffProfileRepository.findByUserId(delivery.getStaffId()).orElse(null);
        if (staff != null) {
            staff.setStaffStatus(StaffStatus.AVAILABLE);
            if (staff.getActiveDeliveryIds() != null)
                staff.getActiveDeliveryIds().remove(delivery.getId());
            staffProfileRepository.save(staff);
        }

        delivery.setStaffId(null);
        delivery.setUpdatedAt(LocalDateTime.now());
        addStatusHistory(delivery, "REJECTED_BY_STAFF",
                request.getStaffId(),
                "Staff từ chối: " + (request.getNote() != null ? request.getNote() : ""));

        deliveryRepository.save(delivery);
        log.info("Delivery {} REJECTED by staff {}", delivery.getId(), request.getStaffId());
        return mapToResponse(delivery);
    }

    @Override
    public DeliveryResponse startDelivery(String deliveryId, DeliveryActionRequest request) {
        Delivery delivery = findDeliveryOrThrow(deliveryId);

        // Nếu đơn đang ở WAITING thì tự động chuyển qua ACCEPTED rồi sang DELIVERING để trải nghiệm mượt mà
        if (delivery.getDeliveryStatus() == DeliveryStatus.WAITING) {
            acceptDelivery(delivery.getId(), request);
            delivery = findDeliveryOrThrow(delivery.getId());
        }

        if (delivery.getDeliveryStatus() != DeliveryStatus.ACCEPTED) {
            throw new IllegalStateException("Chỉ có thể bắt đầu giao khi đơn ở trạng thái ACCEPTED");
        }

        delivery.setDeliveryStatus(DeliveryStatus.DELIVERING);
        delivery.setStartedAt(LocalDateTime.now());
        delivery.setUpdatedAt(LocalDateTime.now());
        addStatusHistory(delivery, DeliveryStatus.DELIVERING.name(),
                request.getStaffId(), "Bắt đầu giao hàng");

        deliveryRepository.save(delivery);

        log.info("Delivery {} STARTED by staff {}", delivery.getId(), request.getStaffId());
        return mapToResponse(delivery);
    }

    @Override
    public DeliveryResponse completeDelivery(String deliveryId, DeliveryActionRequest request) {
        Delivery delivery = findDeliveryOrThrow(deliveryId);

        if (delivery.getDeliveryStatus() == DeliveryStatus.WAITING
                || delivery.getDeliveryStatus() == DeliveryStatus.ACCEPTED) {
            startDelivery(delivery.getId(), request);
            delivery = findDeliveryOrThrow(delivery.getId());
        }

        if (delivery.getDeliveryStatus() != DeliveryStatus.DELIVERING) {
            throw new IllegalStateException("Chỉ có thể hoàn thành khi đơn đang DELIVERING");
        }

        String targetOrderId = delivery.getOrderId();
        Order order = orderRepository.findById(targetOrderId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy đơn hàng: " + targetOrderId));

        // Nếu đơn COD chưa đánh dấu PAID thì tự động xác nhận thu tiền COD khi hoàn tất giao
        if (order.getPaymentStatus() != PaymentStatus.PAID && order.getPaymentMethod() == PaymentMethod.COD) {
            order.setPaymentStatus(PaymentStatus.PAID);
            paymentRepository.findByOrderId(order.getId()).ifPresent(p -> {
                p.setPaymentStatus(PaymentStatus.PAID);
                p.setPaidAmount(order.getFinalAmount());
                p.setConfirmedByStaffId(request.getStaffId());
                p.setConfirmedAt(LocalDateTime.now());
                p.setUpdatedAt(LocalDateTime.now());
                paymentRepository.save(p);
            });
        }

        if (order.getPaymentStatus() != PaymentStatus.PAID) {
            throw new IllegalStateException(
                    "Đơn chưa được thanh toán. PaymentStatus: " + order.getPaymentStatus()
                    + ". Vui lòng xác nhận thu tiền trước.");
        }

        // Kiểm tra Cylinder Exchange: nếu khách đổi vỏ mà chưa báo lỗi thì tự xác nhận thu hồi vỏ đạt chuẩn
        boolean hasExchange = order.getItems() != null && order.getItems().stream()
                .anyMatch(item -> item.getPurchaseType() == PurchaseType.EXCHANGE);

        if (hasExchange) {
            order.getItems().stream()
                    .filter(item -> item.getPurchaseType() == PurchaseType.EXCHANGE)
                    .forEach(item -> {
                        if (item.getCylinderExchangeStatus() != CylinderExchangeStatus.COMPLETED
                                && item.getCylinderExchangeStatus() != CylinderExchangeStatus.NO_CYLINDER) {
                            item.setCylinderExchangeStatus(CylinderExchangeStatus.COMPLETED);
                            if (item.getCylinderCondition() == null) {
                                item.setCylinderCondition(CylinderCondition.NORMAL);
                            }
                        }
                    });
        }

        // Cập nhật Inventory theo từng item
        if (order.getItems() != null) {
            for (Order.OrderItem item : order.getItems()) {
                try {
                    if (item.getPurchaseType() == PurchaseType.EXCHANGE
                            && item.getCylinderExchangeStatus() != CylinderExchangeStatus.NO_CYLINDER) {
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

        updateStaffAfterDelivery(delivery.getStaffId(), delivery.getId(), true);

        log.info("Delivery {} COMPLETED by staff {}", delivery.getId(), request.getStaffId());
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

        updateStaffAfterDelivery(delivery.getStaffId(), delivery.getId(), false);

        log.info("Delivery {} FAILED by staff {}, reason: {}",
                delivery.getId(), request.getStaffId(), request.getFailureReason());
        return mapToResponse(delivery);
    }

    @Override
    public DeliveryResponse processCylinderExchange(String deliveryId, DeliveryActionRequest request) {
        Delivery delivery = findDeliveryOrThrow(deliveryId);

        if (delivery.getDeliveryStatus() == DeliveryStatus.WAITING
                || delivery.getDeliveryStatus() == DeliveryStatus.ACCEPTED) {
            startDelivery(delivery.getId(), request);
            delivery = findDeliveryOrThrow(delivery.getId());
        }

        if (delivery.getDeliveryStatus() != DeliveryStatus.DELIVERING) {
            throw new IllegalStateException("Chỉ xử lý vỏ bình khi đang DELIVERING");
        }

        String targetOrderId = delivery.getOrderId();
        Order order = orderRepository.findById(targetOrderId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy đơn hàng: " + targetOrderId));

        boolean hasNoEmptyCylinder = request.getFailureReason() == DeliveryFailureReason.NO_EMPTY_CYLINDER;

        if (order.getItems() != null) {
            order.getItems().stream()
                    .filter(item -> item.getPurchaseType() == PurchaseType.EXCHANGE)
                    .forEach(item -> {
                        item.setCylinderExchangeStatus(
                                hasNoEmptyCylinder
                                        ? CylinderExchangeStatus.NO_CYLINDER
                                        : CylinderExchangeStatus.COMPLETED);
                        item.setCylinderCondition(
                                request.getCylinderCondition() != null
                                        ? request.getCylinderCondition()
                                        : CylinderCondition.NORMAL);
                    });
        }

        if (request.getAdjustmentFee() != null && request.getAdjustmentFee() != 0) {
            order.setAdjustmentFee(request.getAdjustmentFee());
            order.setAdjustmentNote(request.getAdjustmentNote());
            Double gasSub = order.getGasSubtotal() != null ? order.getGasSubtotal() : 0.0;
            Double cylSub = order.getCylinderFeeTotal() != null ? order.getCylinderFeeTotal() : 0.0;
            Double deliv = order.getDeliveryFee() != null ? order.getDeliveryFee() : 0.0;
            Double disc = order.getDiscountAmount() != null ? order.getDiscountAmount() : 0.0;
            order.setFinalAmount(Math.max(0.0, gasSub + cylSub + deliv - disc + request.getAdjustmentFee()));
        }

        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);

        addStatusHistory(delivery, "CYLINDER_EXCHANGE_PROCESSED",
                request.getStaffId(),
                hasNoEmptyCylinder ? "Khách không có vỏ bình, tính phí bổ sung" : "Thu hồi vỏ bình thành công");
        delivery.setUpdatedAt(LocalDateTime.now());
        deliveryRepository.save(delivery);

        log.info("Cylinder exchange processed for delivery {}", delivery.getId());
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
                .orElseGet(() -> deliveryRepository.findByOrderId(deliveryId)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Không tìm thấy delivery: " + deliveryId)));
    }

    private void addStatusHistory(Delivery delivery, String status, String changedBy, String note) {
        if (delivery.getStatusHistory() == null) delivery.setStatusHistory(new ArrayList<>());
        delivery.getStatusHistory().add(
                Delivery.StatusHistory.builder()
                        .status(status)
                        .note(changedBy + ": " + note)
                        .changedAt(LocalDateTime.now())
                        .build()
        );
    }

    private void updateStaffAfterDelivery(String staffId, String deliveryId, boolean success) {
        if (staffId == null) return;
        staffProfileRepository.findByUserId(staffId).ifPresent(staff -> {
            staff.setTotalDeliveries(staff.getTotalDeliveries() + 1);
            if (success) staff.setSuccessDeliveries(staff.getSuccessDeliveries() + 1);
            else         staff.setFailedDeliveries(staff.getFailedDeliveries() + 1);

            if (staff.getActiveDeliveryIds() != null)
                staff.getActiveDeliveryIds().remove(deliveryId);

            if (staff.getActiveDeliveryIds() == null || staff.getActiveDeliveryIds().isEmpty()) {
                staff.setStaffStatus(StaffStatus.AVAILABLE);
            }

            staffProfileRepository.save(staff);
        });
    }

    private DeliveryResponse mapToResponse(Delivery delivery) {
        String staffName = null;
        if (delivery.getStaffId() != null) {
            staffName = customerProfileRepository.findByUserId(delivery.getStaffId())
                    .map(CustomerProfile::getFullName)
                    .filter(n -> n != null && !n.isBlank())
                    .orElseGet(() -> userRepository.findById(delivery.getStaffId())
                            .map(User::getUsername)
                            .orElse(null));
        }

        Order order = delivery.getOrderId() != null
                ? orderRepository.findById(delivery.getOrderId()).orElse(null)
                : null;
        Payment payment = (order != null && order.getId() != null)
                ? paymentRepository.findByOrderId(order.getId()).orElse(null)
                : null;

        String customerName = null;
        String customerPhone = null;
        if (order != null) {
            if (order.getDeliveryAddressSnapshot() != null) {
                customerName = order.getDeliveryAddressSnapshot().getRecipientName();
                customerPhone = order.getDeliveryAddressSnapshot().getPhone();
            }
            if ((customerName == null || customerName.isBlank()) && order.getCustomerId() != null) {
                customerName = customerProfileRepository.findByUserId(order.getCustomerId())
                        .map(CustomerProfile::getFullName)
                        .orElseGet(() -> userRepository.findById(order.getCustomerId())
                                .map(User::getUsername).orElse("Khách hàng"));
            }
            if ((customerPhone == null || customerPhone.isBlank()) && order.getCustomerId() != null) {
                customerPhone = userRepository.findById(order.getCustomerId())
                        .map(User::getPhone).orElse("");
            }
        }

        return DeliveryResponse.builder()
                .id(delivery.getId())
                .orderId(delivery.getOrderId())
                .orderCode(order != null ? order.getOrderCode() : delivery.getOrderId())
                .staffId(delivery.getStaffId())
                .staffName(staffName)
                .customerName(customerName)
                .customerPhone(customerPhone)
                .deliveryAddressSnapshot(order != null ? order.getDeliveryAddressSnapshot() : null)
                .items(order != null ? order.getItems() : null)
                .orderStatus(order != null ? order.getOrderStatus() : null)
                .approvalStatus(order != null ? order.getApprovalStatus() : null)
                .paymentMethod(order != null ? order.getPaymentMethod() : null)
                .paymentStatus(order != null ? order.getPaymentStatus() : null)
                .paymentId(payment != null ? payment.getId() : null)
                .finalAmount(order != null ? order.getFinalAmount() : null)
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
