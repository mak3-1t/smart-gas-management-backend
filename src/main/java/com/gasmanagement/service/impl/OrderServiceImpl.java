package com.gasmanagement.service.impl;

import com.gasmanagement.dto.request.CancelOrderRequest;
import com.gasmanagement.dto.request.CreateOrderRequest;
import com.gasmanagement.dto.response.OrderResponse;
import com.gasmanagement.exception.BusinessException;
import com.gasmanagement.exception.ResourceNotFoundException;
import com.gasmanagement.model.*;
import com.gasmanagement.model.enums.*;
import com.gasmanagement.repository.*;
import com.gasmanagement.service.OrderService;
import com.gasmanagement.service.StoreSettingService;
import com.gasmanagement.service.VoucherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final BrandRepository brandRepository;
    private final AddressRepository addressRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final UserRepository userRepository;
    private final InventoryRepository inventoryRepository;
    private final PaymentRepository paymentRepository;
    private final RefundRepository refundRepository;
    private final DeliveryRepository deliveryRepository;
    private final StaffProfileRepository staffProfileRepository;
    private final VoucherService voucherService;
    private final StoreSettingService storeSettingService;
    private final MongoTemplate mongoTemplate;

    private static final SecureRandom RANDOM = new SecureRandom();

    // Tài khoản nhận chuyển khoản – dùng chung cấu hình với PayOS (application.yml → payment.bank.*)
    @Value("${payment.bank.bin}")
    private String bankBin;

    @Value("${payment.bank.code}")
    private String bankCode;

    @Value("${payment.bank.account-number}")
    private String bankAccountNumber;

    @Value("${payment.bank.account-name}")
    private String bankAccountName;

    @Override
    public OrderResponse createOrder(String customerId, CreateOrderRequest request) {
        Cart cart = cartRepository.findByUserId(customerId)
                .orElseThrow(() -> new BusinessException("Giỏ hàng của bạn đang trống"));

        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new BusinessException("Giỏ hàng của bạn đang trống");
        }

        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin khách hàng"));

        // 1. Kiểm tra tồn kho & tạo snapshot OrderItem
        List<Order.OrderItem> orderItems = new ArrayList<>();
        double gasSubtotal = 0.0;
        double cylinderFeeTotal = 0.0;

        for (Cart.CartItem item : cart.getItems()) {
            Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm không tồn tại: " + item.getProductId()));

            if (product.getStatus() != ProductStatus.ACTIVE) {
                throw new BusinessException("Sản phẩm '" + product.getName() + "' hiện không mở bán");
            }

            Inventory inventory = inventoryRepository.findByProductId(product.getId()).orElse(null);
            int available = inventory != null ? inventory.getAvailableCylinder() : 0;
            if (available < item.getQuantity()) {
                throw new BusinessException("Sản phẩm '" + product.getName() + "' chỉ còn " + available + " bình trong kho");
            }

            String brandName = null;
            if (product.getBrandId() != null) {
                brandName = brandRepository.findById(product.getBrandId())
                        .map(Brand::getName).orElse(null);
            }

            double unitPrice = product.getGasPrice() != null ? product.getGasPrice() : 0.0;
            double cylinderFee = (item.getPurchaseType() == PurchaseType.NEW_CYLINDER && product.getCylinderFee() != null)
                    ? product.getCylinderFee() : 0.0;

            gasSubtotal += unitPrice * item.getQuantity();
            cylinderFeeTotal += cylinderFee * item.getQuantity();

            orderItems.add(Order.OrderItem.builder()
                    .productId(product.getId())
                    .productName(product.getName())
                    .brandName(brandName)
                    .weightKg(product.getWeightKg())
                    .purchaseType(item.getPurchaseType())
                    .quantity(item.getQuantity())
                    .unitPriceAtOrderTime(unitPrice)
                    .cylinderFeeAtOrderTime(cylinderFee)
                    .cylinderExchangeStatus(item.getPurchaseType() == PurchaseType.EXCHANGE
                            ? CylinderExchangeStatus.PENDING : null)
                    .cylinderCondition(CylinderCondition.NORMAL)
                    .build());
        }

        // 2. Snapshot địa chỉ giao hàng
        Order.AddressSnapshot addressSnapshot = resolveAddressSnapshot(customerId, request);

        // 3. Kiểm tra Delivery Type & Scheduled Delivery Rule
        LocalDateTime scheduledTime = null;
        if (request.getDeliveryType() == DeliveryType.SCHEDULED_DELIVERY) {
            scheduledTime = request.getScheduledDeliveryTime();
            if (scheduledTime == null) {
                throw new BusinessException("Vui lòng chọn thời gian giao hàng mong muốn");
            }
            if (scheduledTime.isBefore(LocalDateTime.now())) {
                throw new BusinessException("Thời gian giao hàng không được ở trong quá khứ");
            }
            if (scheduledTime.isBefore(LocalDateTime.now().plusMinutes(30))) {
                throw new BusinessException("Thời gian giao hàng hẹn trước phải cách hiện tại ít nhất 30 phút");
            }

            StoreSetting settings = storeSettingService.getSettings();
            if (!isWithinOperatingHours(scheduledTime, settings.getStoreOperatingHours())) {
                throw new BusinessException("Thời gian giao hàng nằm ngoài khung giờ mở cửa ("
                        + settings.getStoreOperatingHours() + ")");
            }
        }

        // 4. Tính toán phí vận chuyển & Giảm giá
        double subtotal = gasSubtotal + cylinderFeeTotal;
        StoreSetting settings = storeSettingService.getSettings();
        double defaultDeliveryFee = settings.getDefaultDeliveryFee() != null ? settings.getDefaultDeliveryFee() : 30000.0;
        double freeThreshold = settings.getFreeShippingThreshold() != null ? settings.getFreeShippingThreshold() : 1000000.0;
        double deliveryFee = (subtotal >= freeThreshold) ? 0.0 : defaultDeliveryFee;

        // Voucher: chỉ TÍNH giảm giá ở đây, việc tăng lượt dùng thực hiện SAU khi lưu đơn thành công
        String voucherCode = request.getVoucherCode() != null && !request.getVoucherCode().trim().isEmpty()
                ? request.getVoucherCode().trim() : cart.getVoucherCode();

        double discountAmount = 0.0;
        if (voucherCode != null && !voucherCode.isEmpty()) {
            try {
                discountAmount = voucherService.calculateDiscount(voucherCode, subtotal);
            } catch (Exception e) {
                log.warn("Không thể áp dụng voucher {}: {}", voucherCode, e.getMessage());
                voucherCode = null;
                discountAmount = 0.0;
            }
        }

        double finalAmount = Math.max(0.0, subtotal + deliveryFee - discountAmount);

        // 5. Tạo Order
        LocalDateTime now = LocalDateTime.now();
        String orderCode = generateOrderCode();

        List<Order.StatusHistory> statusHistories = new ArrayList<>();
        statusHistories.add(Order.StatusHistory.builder()
                .status(OrderStatus.ORDER_PLACED.name())
                .changedBy(customerId)
                .note("Khách hàng đặt hàng thành công")
                .changedAt(now)
                .build());

        Order order = Order.builder()
                .orderCode(orderCode)
                .customerId(customerId)
                .orderStatus(OrderStatus.ORDER_PLACED)
                .approvalStatus(ApprovalStatus.PENDING_APPROVAL)
                .paymentStatus(PaymentStatus.PENDING)
                .paymentMethod(request.getPaymentMethod())
                .deliveryType(request.getDeliveryType())
                .scheduledDeliveryTime(scheduledTime)
                .deliveryAddressSnapshot(addressSnapshot)
                .items(orderItems)
                .voucherCode(voucherCode)
                .gasSubtotal(gasSubtotal)
                .cylinderFeeTotal(cylinderFeeTotal)
                .deliveryFee(deliveryFee)
                .discountAmount(discountAmount)
                .adjustmentFee(0.0)
                .finalAmount(finalAmount)
                .statusHistory(statusHistories)
                .approvalHistory(new ArrayList<>())
                .createdAt(now)
                .updatedAt(now)
                .build();

        // Lưu Order + Payment + tăng lượt voucher. MongoDB standalone không hỗ trợ transaction,
        // nên dùng cơ chế bù trừ (compensation): lỗi ở bước nào thì hoàn tác các bước đã làm.
        Order savedOrder = orderRepository.save(order);
        Payment savedPayment = null;
        try {
            savedPayment = initPaymentRecord(savedOrder, customer);
            if (voucherCode != null) {
                voucherService.incrementUsage(voucherCode);
            }
        } catch (RuntimeException e) {
            log.error("Tạo đơn thất bại, hoàn tác orderId={}: {}", savedOrder.getId(), e.getMessage());
            if (savedPayment != null) {
                paymentRepository.delete(savedPayment);
            }
            orderRepository.delete(savedOrder);
            throw new BusinessException("Không thể tạo đơn hàng, vui lòng thử lại: " + e.getMessage());
        }
        log.info("Order created: orderId={}, orderCode={}, finalAmount={}",
                savedOrder.getId(), savedOrder.getOrderCode(), savedOrder.getFinalAmount());

        // 7. Clear giỏ hàng của Customer sau khi đặt (không ảnh hưởng tính đúng đắn của đơn)
        cart.setItems(new ArrayList<>());
        cart.setVoucherCode(null);
        cart.setVoucherId(null);
        cart.setUpdatedAt(now);
        cartRepository.save(cart);

        return mapToResponse(savedOrder);
    }

    @Override
    public Page<OrderResponse> getMyOrders(String customerId, OrderStatus status, ApprovalStatus approvalStatus, Pageable pageable) {
        Query query = new Query();
        query.addCriteria(Criteria.where("customerId").is(customerId));

        if (status != null) {
            query.addCriteria(Criteria.where("orderStatus").is(status));
        }
        if (approvalStatus != null) {
            query.addCriteria(Criteria.where("approvalStatus").is(approvalStatus));
        }

        long total = mongoTemplate.count(query, Order.class);
        query.with(pageable);
        List<Order> orders = mongoTemplate.find(query, Order.class);

        return new PageImpl<>(mapToResponses(orders), pageable, total);
    }

    @Override
    public OrderResponse getOrderById(String orderId, String userId, boolean isManagerOrStaff) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng: " + orderId));

        if (!isManagerOrStaff && !order.getCustomerId().equals(userId)) {
            throw new BusinessException("Bạn không có quyền xem đơn hàng này");
        }

        return mapToResponse(order);
    }

    @Override
    public OrderResponse cancelOrder(String orderId, String customerId, CancelOrderRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng: " + orderId));

        if (!order.getCustomerId().equals(customerId)) {
            throw new BusinessException("Bạn không có quyền hủy đơn hàng này");
        }

        if (order.getApprovalStatus() != ApprovalStatus.PENDING_APPROVAL) {
            throw new BusinessException("Chỉ có thể hủy đơn hàng khi đơn chưa được Manager duyệt");
        }

        if (order.getOrderStatus() == OrderStatus.CANCELLED) {
            throw new BusinessException("Đơn hàng này đã được hủy trước đó");
        }

        LocalDateTime now = LocalDateTime.now();
        order.setOrderStatus(OrderStatus.CANCELLED);
        order.setCancelReason(request.getReason());
        order.setUpdatedAt(now);

        if (order.getStatusHistory() == null) {
            order.setStatusHistory(new ArrayList<>());
        }
        order.getStatusHistory().add(Order.StatusHistory.builder()
                .status(OrderStatus.CANCELLED.name())
                .changedBy(customerId)
                .note("Khách hàng hủy đơn: " + request.getReason())
                .changedAt(now)
                .build());

        // Nếu khách đã thanh toán trước (Online/Transfer), đưa vào trạng thái REFUND_PENDING
        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            order.setPaymentStatus(PaymentStatus.REFUND_PENDING);

            Payment payment = paymentRepository.findByOrderId(order.getId()).orElse(null);
            Refund refund = Refund.builder()
                    .orderId(order.getId())
                    .paymentId(payment != null ? payment.getId() : null)
                    .customerId(customerId)
                    .amount(order.getFinalAmount())
                    .reason(request.getReason())
                    .status(RefundStatus.PENDING)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();
            refundRepository.save(refund);
            log.info("Tạo yêu cầu hoàn tiền cho đơn đã hủy: orderId={}, refundId={}", orderId, refund.getId());
        }

        Order savedOrder = orderRepository.save(order);
        log.info("Khách hàng {} đã hủy đơn hàng {}", customerId, orderId);
        return mapToResponse(savedOrder);
    }

    @Override
    public Page<OrderResponse> getAllOrdersForManager(OrderStatus status, ApprovalStatus approvalStatus, Pageable pageable) {
        Query query = new Query();
        if (status != null) {
            query.addCriteria(Criteria.where("orderStatus").is(status));
        }
        if (approvalStatus != null) {
            query.addCriteria(Criteria.where("approvalStatus").is(approvalStatus));
        }

        long total = mongoTemplate.count(query, Order.class);
        query.with(pageable);
        List<Order> orders = mongoTemplate.find(query, Order.class);

        return new PageImpl<>(mapToResponses(orders), pageable, total);
    }

    // ─────────────── PRIVATE HELPERS ───────────────

    private Order.AddressSnapshot resolveAddressSnapshot(String customerId, CreateOrderRequest request) {
        if (request.getAddressId() != null && !request.getAddressId().trim().isEmpty()) {
            Address address = addressRepository.findByIdAndUserId(request.getAddressId(), customerId)
                    .orElseThrow(() -> new BusinessException("Địa chỉ đã chọn không hợp lệ"));
            return Order.AddressSnapshot.builder()
                    .recipientName(address.getRecipientName())
                    .phone(address.getPhone())
                    .addressLine(address.getAddressLine())
                    .ward(address.getWard())
                    .district(address.getDistrict())
                    .city(address.getCity())
                    .build();
        }

        if (request.getCustomAddress() != null) {
            Order.AddressSnapshot custom = request.getCustomAddress();
            if (custom.getRecipientName() == null || custom.getRecipientName().isBlank()
                    || custom.getPhone() == null || custom.getPhone().isBlank()
                    || custom.getAddressLine() == null || custom.getAddressLine().isBlank()) {
                throw new BusinessException("Thông tin địa chỉ giao hàng chưa đầy đủ");
            }
            return custom;
        }

        // Lấy địa chỉ mặc định
        Address defaultAddress = addressRepository.findByUserIdAndIsDefaultTrue(customerId)
                .orElseGet(() -> {
                    List<Address> list = addressRepository.findByUserId(customerId);
                    return list.isEmpty() ? null : list.get(0);
                });

        if (defaultAddress != null) {
            return Order.AddressSnapshot.builder()
                    .recipientName(defaultAddress.getRecipientName())
                    .phone(defaultAddress.getPhone())
                    .addressLine(defaultAddress.getAddressLine())
                    .ward(defaultAddress.getWard())
                    .district(defaultAddress.getDistrict())
                    .city(defaultAddress.getCity())
                    .build();
        }

        throw new BusinessException("Vui lòng cung cấp địa chỉ giao hàng");
    }

    private Payment initPaymentRecord(Order order, User customer) {
        LocalDateTime now = LocalDateTime.now();
        Payment payment = Payment.builder()
                .orderId(order.getId())
                .customerId(order.getCustomerId())
                .paymentMethod(order.getPaymentMethod())
                .paymentStatus(PaymentStatus.PENDING)
                .amount(order.getFinalAmount())
                .paidAmount(0.0)
                .transactions(new ArrayList<>())
                .createdAt(now)
                .updatedAt(now)
                .build();

        if (order.getPaymentMethod() == PaymentMethod.BANK_TRANSFER) {
            String bankAccount = bankCode + " - " + bankAccountNumber + " (" + bankAccountName + ")";
            String transferContent = "GAS " + order.getOrderCode();
            String qrUrl = "https://img.vietqr.io/image/" + bankBin + "-" + bankAccountNumber + "-compact2.png"
                    + "?amount=" + order.getFinalAmount().longValue()
                    + "&addInfo=" + URLEncoder.encode(transferContent, StandardCharsets.UTF_8)
                    + "&accountName=" + URLEncoder.encode(bankAccountName, StandardCharsets.UTF_8);

            payment.setBankAccount(bankAccount);
            payment.setTransferContent(transferContent);
            payment.setQrCodeUrl(qrUrl);
        }

        return paymentRepository.save(payment);
    }

    /**
     * Sinh mã đơn dạng GAS-yyMMdd-XXXXXX (6 ký tự base36 ngẫu nhiên, ~2 tỷ tổ hợp/ngày)
     * và kiểm tra trùng trong DB trước khi dùng.
     */
    private String generateOrderCode() {
        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMdd"));
        final String alphabet = "0123456789ABCDEFGHJKLMNPQRSTUVWXYZ";
        for (int attempt = 0; attempt < 10; attempt++) {
            StringBuilder sb = new StringBuilder(6);
            for (int i = 0; i < 6; i++) {
                sb.append(alphabet.charAt(RANDOM.nextInt(alphabet.length())));
            }
            String code = "GAS-" + datePart + "-" + sb;
            boolean exists = mongoTemplate.exists(Query.query(Criteria.where("orderCode").is(code)), Order.class);
            if (!exists) {
                return code;
            }
        }
        throw new BusinessException("Không thể sinh mã đơn hàng, vui lòng thử lại");
    }

    private boolean isWithinOperatingHours(LocalDateTime scheduledTime, String hoursRange) {
        if (hoursRange == null || !hoursRange.contains("-")) {
            return true;
        }
        try {
            String[] parts = hoursRange.split("-");
            LocalTime open = LocalTime.parse(parts[0].trim());
            LocalTime close = LocalTime.parse(parts[1].trim());
            LocalTime target = scheduledTime.toLocalTime();
            return !target.isBefore(open) && !target.isAfter(close);
        } catch (Exception e) {
            log.warn("Không thể parse storeOperatingHours: {}", hoursRange);
            return true;
        }
    }

    /** Map 1 đơn (dùng cho chi tiết / tạo / hủy). */
    private OrderResponse mapToResponse(Order order) {
        return mapToResponses(List.of(order)).get(0);
    }

    /**
     * Map danh sách đơn với số truy vấn cố định (5 query) thay vì 4-5 query cho MỖI đơn (N+1).
     */
    private List<OrderResponse> mapToResponses(List<Order> orders) {
        if (orders == null || orders.isEmpty()) {
            return new ArrayList<>();
        }

        Set<String> orderIds = orders.stream().map(Order::getId).collect(Collectors.toSet());
        Set<String> customerIds = orders.stream().map(Order::getCustomerId)
                .filter(Objects::nonNull).collect(Collectors.toSet());

        Map<String, Delivery> deliveryByOrder = mongoTemplate
                .find(Query.query(Criteria.where("orderId").in(orderIds)), Delivery.class).stream()
                .collect(Collectors.toMap(Delivery::getOrderId, Function.identity(), (a, b) -> b));

        Map<String, Payment> paymentByOrder = mongoTemplate
                .find(Query.query(Criteria.where("orderId").in(orderIds)), Payment.class).stream()
                .collect(Collectors.toMap(Payment::getOrderId, Function.identity(), (a, b) -> b));

        Set<String> userIds = new HashSet<>(customerIds);
        deliveryByOrder.values().stream().map(Delivery::getStaffId)
                .filter(Objects::nonNull).forEach(userIds::add);

        Map<String, CustomerProfile> profileByUser = mongoTemplate
                .find(Query.query(Criteria.where("userId").in(userIds)), CustomerProfile.class).stream()
                .collect(Collectors.toMap(CustomerProfile::getUserId, Function.identity(), (a, b) -> b));

        Map<String, User> userById = new HashMap<>();
        userRepository.findAllById(userIds).forEach(u -> userById.put(u.getId(), u));

        List<OrderResponse> result = new ArrayList<>(orders.size());
        for (Order order : orders) {
            result.add(buildResponse(order,
                    profileByUser.get(order.getCustomerId()),
                    userById.get(order.getCustomerId()),
                    deliveryByOrder.get(order.getId()),
                    paymentByOrder.get(order.getId()),
                    userById,
                    profileByUser));
        }
        return result;
    }

    private OrderResponse buildResponse(Order order, CustomerProfile profile, User user,
                                        Delivery delivery, Payment payment, Map<String, User> userById,
                                        Map<String, CustomerProfile> profileByUser) {
        String staffName = null;
        String staffPhone = null;

        if (delivery != null && delivery.getStaffId() != null) {
            User staffUser = userById.get(delivery.getStaffId());
            CustomerProfile staffProf = profileByUser != null ? profileByUser.get(delivery.getStaffId()) : null;
            if (staffProf != null && staffProf.getFullName() != null && !staffProf.getFullName().isBlank()) {
                staffName = staffProf.getFullName();
            } else if (staffUser != null) {
                staffName = staffUser.getUsername();
            }
            if (staffUser != null) {
                staffPhone = staffUser.getPhone();
            }
        }

        return OrderResponse.builder()
                .id(order.getId())
                .orderCode(order.getOrderCode())
                .customerId(order.getCustomerId())
                .customerName(profile != null ? profile.getFullName() : (user != null ? user.getUsername() : null))
                .customerPhone(user != null ? user.getPhone() : null)
                .orderStatus(order.getOrderStatus())
                .approvalStatus(order.getApprovalStatus())
                .paymentStatus(order.getPaymentStatus())
                .paymentMethod(order.getPaymentMethod())
                .deliveryType(order.getDeliveryType())
                .scheduledDeliveryTime(order.getScheduledDeliveryTime())
                .deliveryAddressSnapshot(order.getDeliveryAddressSnapshot())
                .items(order.getItems())
                .voucherCode(order.getVoucherCode())
                .gasSubtotal(order.getGasSubtotal())
                .cylinderFeeTotal(order.getCylinderFeeTotal())
                .deliveryFee(order.getDeliveryFee())
                .discountAmount(order.getDiscountAmount())
                .adjustmentFee(order.getAdjustmentFee())
                .adjustmentNote(order.getAdjustmentNote())
                .finalAmount(order.getFinalAmount())
                .statusHistory(order.getStatusHistory())
                .approvalHistory(order.getApprovalHistory())
                .cancelReason(order.getCancelReason())
                .deliveryId(delivery != null ? delivery.getId() : null)
                .deliveryStatus(delivery != null ? delivery.getDeliveryStatus() : null)
                .staffId(delivery != null ? delivery.getStaffId() : null)
                .staffName(staffName)
                .staffPhone(staffPhone)
                .paymentId(payment != null ? payment.getId() : null)
                .qrCodeUrl(payment != null ? payment.getQrCodeUrl() : null)
                .bankAccount(payment != null ? payment.getBankAccount() : null)
                .transferContent(payment != null ? payment.getTransferContent() : null)
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }
}
