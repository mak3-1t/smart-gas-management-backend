package com.gasmanagement.service.impl;

import com.gasmanagement.dto.response.dashboard.*;
import com.gasmanagement.model.Inventory;
import com.gasmanagement.model.StaffProfile;
import com.gasmanagement.model.User;
import com.gasmanagement.model.enums.*;
import com.gasmanagement.repository.*;
import com.gasmanagement.service.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implement Dashboard aggregations.
 *
 * <p><b>Revenue Rule:</b> Chỉ tính đơn {@code OrderStatus=DELIVERED AND PaymentStatus=PAID}.
 *
 * <p>Dùng {@link MongoTemplate} Aggregation Pipeline cho các query phức tạp
 * (unwind items, group, sum). Dùng Repository cho các count đơn giản.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final OrderRepository orderRepository;
    private final DeliveryRepository deliveryRepository;
    private final StaffProfileRepository staffProfileRepository;
    private final InventoryRepository inventoryRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final MongoTemplate mongoTemplate;

    /** Ngưỡng cảnh báo tồn kho thấp mặc định */
    private static final int DEFAULT_LOW_STOCK_THRESHOLD = 5;

    // ─────────────── MAIN SUMMARY ───────────────

    @Override
    public DashboardSummaryResponse getDashboardSummary() {
        log.info("Building dashboard summary");

        // Delivery counts
        long activeDeliveries =
                deliveryRepository.countByDeliveryStatus(DeliveryStatus.ACCEPTED)
                + deliveryRepository.countByDeliveryStatus(DeliveryStatus.DELIVERING);

        return DashboardSummaryResponse.builder()

                // Revenue block
                .revenue(buildRevenueStats())

                // Order counts
                .totalOrders(orderRepository.count())
                .ordersPendingApproval(orderRepository.countByApprovalStatus(ApprovalStatus.PENDING_APPROVAL))
                .ordersPaid(orderRepository.countByPaymentStatus(PaymentStatus.PAID))
                .ordersDelivered(orderRepository.countByOrderStatus(OrderStatus.DELIVERED))
                .ordersCancelled(orderRepository.countByOrderStatus(OrderStatus.CANCELLED))

                // Delivery counts
                .totalDeliveries(deliveryRepository.count())
                .failedDeliveries(deliveryRepository.countByDeliveryStatus(DeliveryStatus.FAILED))
                .activeDeliveries(activeDeliveries)

                // Staff counts
                .availableStaff(staffProfileRepository.countByStaffStatus(StaffStatus.AVAILABLE))
                .busyStaff(staffProfileRepository.countByStaffStatus(StaffStatus.BUSY))
                .offlineStaff(staffProfileRepository.countByStaffStatus(StaffStatus.OFFLINE))

                // Products sold
                .totalProductsSold(calculateTotalProductsSold())

                // Lists
                .topSellingProducts(getTopSellingProducts(10))
                .slowSellingProducts(getSlowSellingProducts(10))
                .revenueByProduct(getRevenueByProduct(10))
                .staffPerformance(getStaffPerformance())
                .lowStockProducts(getLowStockProducts(DEFAULT_LOW_STOCK_THRESHOLD))

                .build();
    }

    // ─────────────── REVENUE ───────────────

    /**
     * Tổng hợp doanh thu theo các kỳ thời gian.
     */
    private RevenueStats buildRevenueStats() {
        LocalDateTime today     = LocalDate.now().atStartOfDay();
        LocalDateTime weekStart = LocalDate.now().with(DayOfWeek.MONDAY).atStartOfDay();
        LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();

        long revenueOrderCount = orderRepository.countByOrderStatus(OrderStatus.DELIVERED);

        return RevenueStats.builder()
                .totalRevenue(aggregateRevenue(null, null))
                .revenueToday(aggregateRevenue(today, null))
                .revenueThisWeek(aggregateRevenue(weekStart, null))
                .revenueThisMonth(aggregateRevenue(monthStart, null))
                .revenueOrderCount(revenueOrderCount)
                .build();
    }

    /**
     * Dùng MongoDB Aggregation để sum finalAmount trên đơn DELIVERED + PAID.
     *
     * @param from ngày bắt đầu (null = không giới hạn)
     * @param to   ngày kết thúc (null = không giới hạn)
     * @return tổng doanh thu
     */
    private double aggregateRevenue(LocalDateTime from, LocalDateTime to) {
        try {
            Criteria criteria = Criteria
                    .where("orderStatus").is(OrderStatus.DELIVERED.name())
                    .and("paymentStatus").is(PaymentStatus.PAID.name());

            if (from != null) criteria = criteria.and("createdAt").gte(from);
            if (to != null)   criteria = criteria.lte(to);

            MatchOperation match = Aggregation.match(criteria);
            GroupOperation  group = Aggregation.group()
                    .sum("finalAmount").as("total");

            AggregationResults<Document> results = mongoTemplate.aggregate(
                    Aggregation.newAggregation(match, group), "orders", Document.class);

            Document doc = results.getUniqueMappedResult();
            if (doc == null) return 0.0;

            Object total = doc.get("total");
            return total instanceof Number ? ((Number) total).doubleValue() : 0.0;

        } catch (Exception e) {
            log.error("Revenue aggregation error: {}", e.getMessage());
            return 0.0;
        }
    }

    // ─────────────── PRODUCTS SOLD ───────────────

    /**
     * Tổng số lượng bình đã giao thành công (DELIVERED + PAID).
     */
    private long calculateTotalProductsSold() {
        try {
            MatchOperation match = Aggregation.match(
                    Criteria.where("orderStatus").is(OrderStatus.DELIVERED.name())
                            .and("paymentStatus").is(PaymentStatus.PAID.name()));

            UnwindOperation unwind = Aggregation.unwind("items");

            GroupOperation group = Aggregation.group()
                    .sum("items.quantity").as("total");

            AggregationResults<Document> results = mongoTemplate.aggregate(
                    Aggregation.newAggregation(match, unwind, group), "orders", Document.class);

            Document doc = results.getUniqueMappedResult();
            if (doc == null) return 0L;

            Object total = doc.get("total");
            return total instanceof Number ? ((Number) total).longValue() : 0L;

        } catch (Exception e) {
            log.error("Total products sold aggregation error: {}", e.getMessage());
            return 0L;
        }
    }

    // ─────────────── TOP / SLOW SELLING ───────────────

    @Override
    public List<ProductSalesStats> getTopSellingProducts(int limit) {
        return aggregateProductSales(limit, Sort.Direction.DESC);
    }

    @Override
    public List<ProductSalesStats> getSlowSellingProducts(int limit) {
        return aggregateProductSales(limit, Sort.Direction.ASC);
    }

    /**
     * Aggregation pipeline:
     * <ol>
     *   <li>Match DELIVERED + PAID</li>
     *   <li>Unwind items</li>
     *   <li>Group by items.productId → sum quantity + sum revenue</li>
     *   <li>Sort by totalQuantitySold</li>
     *   <li>Limit</li>
     * </ol>
     */
    private List<ProductSalesStats> aggregateProductSales(int limit, Sort.Direction direction) {
        try {
            MatchOperation match = Aggregation.match(
                    Criteria.where("orderStatus").is(OrderStatus.DELIVERED.name())
                            .and("paymentStatus").is(PaymentStatus.PAID.name()));

            UnwindOperation unwind = Aggregation.unwind("items");

            GroupOperation group = Aggregation.group("items.productId")
                    .sum("items.quantity").as("totalQuantitySold")
                    .first("items.productName").as("productName")
                    .first("items.brandName").as("brandName")
                    .sum(ArithmeticOperators.Multiply
                            .valueOf("items.unitPriceAtOrderTime")
                            .multiplyBy("items.quantity")).as("totalRevenue");

            SortOperation sort = Aggregation.sort(direction, "totalQuantitySold");
            LimitOperation limitOp = Aggregation.limit(limit);

            AggregationResults<Document> results = mongoTemplate.aggregate(
                    Aggregation.newAggregation(match, unwind, group, sort, limitOp),
                    "orders", Document.class);

            return results.getMappedResults().stream()
                    .map(this::documentToProductSalesStats)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Product sales aggregation error: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public List<ProductSalesStats> getRevenueByProduct(int limit) {
        try {
            MatchOperation match = Aggregation.match(
                    Criteria.where("orderStatus").is(OrderStatus.DELIVERED.name())
                            .and("paymentStatus").is(PaymentStatus.PAID.name()));

            UnwindOperation unwind = Aggregation.unwind("items");

            GroupOperation group = Aggregation.group("items.productId")
                    .sum("items.quantity").as("totalQuantitySold")
                    .first("items.productName").as("productName")
                    .first("items.brandName").as("brandName")
                    .sum(ArithmeticOperators.Multiply
                            .valueOf("items.unitPriceAtOrderTime")
                            .multiplyBy("items.quantity")).as("totalRevenue");

            SortOperation sort = Aggregation.sort(Sort.Direction.DESC, "totalRevenue");
            LimitOperation limitOp = Aggregation.limit(limit);

            AggregationResults<Document> results = mongoTemplate.aggregate(
                    Aggregation.newAggregation(match, unwind, group, sort, limitOp),
                    "orders", Document.class);

            return results.getMappedResults().stream()
                    .map(this::documentToProductSalesStats)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Revenue by product aggregation error: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    // ─────────────── STAFF PERFORMANCE ───────────────

    @Override
    public List<StaffPerformanceStats> getStaffPerformance() {
        List<StaffProfile> profiles = staffProfileRepository.findAll();

        // Preload users để tránh N+1 queries
        Map<String, User> userMap = new HashMap<>();
        profiles.forEach(p -> {
            if (p.getUserId() != null) {
                userRepository.findById(p.getUserId())
                        .ifPresent(u -> userMap.put(p.getUserId(), u));
            }
        });

        return profiles.stream()
                .map(profile -> {
                    User user = userMap.get(profile.getUserId());
                    double successRate = profile.getTotalDeliveries() == 0 ? 0.0
                            : (double) profile.getSuccessDeliveries() / profile.getTotalDeliveries() * 100;

                    return StaffPerformanceStats.builder()
                            .staffId(profile.getUserId())
                            .username(user != null ? user.getUsername() : null)
                            .email(user != null ? user.getEmail() : null)
                            .totalDeliveries(profile.getTotalDeliveries())
                            .successDeliveries(profile.getSuccessDeliveries())
                            .failedDeliveries(profile.getFailedDeliveries())
                            .successRate(Math.round(successRate * 10.0) / 10.0)
                            .currentStatus(profile.getStaffStatus().name())
                            .build();
                })
                .sorted(Comparator.comparingDouble(StaffPerformanceStats::getSuccessRate).reversed())
                .collect(Collectors.toList());
    }

    // ─────────────── LOW STOCK ───────────────

    @Override
    public List<LowStockStats> getLowStockProducts(int threshold) {
        // Preload all products into a map for quick lookup
        Map<String, String> productNameMap = productRepository.findAll().stream()
                .collect(Collectors.toMap(
                        p -> p.getId(),
                        p -> p.getName(),
                        (a, b) -> a));

        return inventoryRepository.findAll().stream()
                .filter(inv -> inv.getAvailableCylinder() < threshold)
                .map(inv -> LowStockStats.builder()
                        .productId(inv.getProductId())
                        .productName(productNameMap.getOrDefault(inv.getProductId(), "Unknown"))
                        .fullCylinder(inv.getFullCylinder())
                        .reservedCylinder(inv.getReservedCylinder())
                        .availableCylinder(inv.getAvailableCylinder())
                        .emptyCylinder(inv.getEmptyCylinder())
                        .damagedCylinder(inv.getDamagedCylinder())
                        .build())
                .sorted(Comparator.comparingInt(LowStockStats::getAvailableCylinder))
                .collect(Collectors.toList());
    }

    // ─────────────── HELPER ───────────────

    private ProductSalesStats documentToProductSalesStats(Document doc) {
        Object idObj = doc.get("_id");
        Object qtyObj = doc.get("totalQuantitySold");
        Object revObj = doc.get("totalRevenue");

        return ProductSalesStats.builder()
                .productId(idObj != null ? idObj.toString() : null)
                .productName(doc.getString("productName"))
                .brandName(doc.getString("brandName"))
                .totalQuantitySold(qtyObj instanceof Number ? ((Number) qtyObj).longValue() : 0L)
                .totalRevenue(revObj instanceof Number ? ((Number) revObj).doubleValue() : 0.0)
                .build();
    }
}
