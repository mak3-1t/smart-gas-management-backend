package com.gasmanagement.config;

import com.gasmanagement.model.*;
import com.gasmanagement.model.enums.*;
import com.gasmanagement.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Tự động khởi tạo dữ liệu mẫu chuẩn cho 4 vai trò (CUSTOMER, STAFF, MANAGER, SYSTEM_ADMIN)
 * và nới lỏng schema validator của MongoDB để đồng bộ kiểu String ID của Spring Data MongoDB.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final StaffProfileRepository staffProfileRepository;
    private final AddressRepository addressRepository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final DeliveryRepository deliveryRepository;
    private final StoreSettingRepository storeSettingRepository;
    private final PasswordEncoder passwordEncoder;
    private final MongoTemplate mongoTemplate;

    @Override
    public void run(String... args) {
        try {
            relaxMongoValidators();
            seedDefaultAccounts();
            seedDefaultCatalogAndInventory();
        } catch (Exception e) {
            log.warn("DataInitializer warning: {}", e.getMessage());
        }
    }

    /**
     * Chuyển validationAction của các collection sang 'warn' để tránh xung đột giữa
     * String ID của Spring Data MongoDB và bsonType: 'objectId' trong init-database.js
     */
    private void relaxMongoValidators() {
        String[] collections = {
            "users", "customer_profiles", "staff_profiles", "addresses",
            "categories", "brands", "products", "inventories", "inventory_transactions",
            "vouchers", "voucher_usages", "carts", "orders", "payments", "refunds",
            "deliveries", "delivery_batches", "reviews", "notifications", "store_settings", "audit_logs"
        };
        for (String col : collections) {
            try {
                if (mongoTemplate.collectionExists(col)) {
                    mongoTemplate.executeCommand(new Document("collMod", col)
                            .append("validationLevel", "moderate")
                            .append("validationAction", "warn"));
                }
            } catch (Exception ignored) {
            }
        }
    }

    private void seedDefaultAccounts() {
        LocalDateTime now = LocalDateTime.now();
        String defaultHash = passwordEncoder.encode("password123");

        // Sửa tài khoản sysadmin nếu đang bị gán chuỗi CHANGE_ME_AFTER_FIRST_LOGIN
        userRepository.findByUsername("sysadmin").ifPresent(sys -> {
            if (sys.getPasswordHash() == null || !sys.getPasswordHash().startsWith("$2")) {
                sys.setPasswordHash(defaultHash);
                sys.setUpdatedAt(now);
                userRepository.save(sys);
                log.info("Repaired passwordHash for user 'sysadmin' (password: password123)");
            }
        });

        // 1. CUSTOMER
        User customer = ensureUser("customer", "customer@gasflow.vn", "0901234567", defaultHash, UserRole.CUSTOMER, now);
        if (customerProfileRepository.findByUserId(customer.getId()).isEmpty()) {
            customerProfileRepository.save(CustomerProfile.builder()
                    .userId(customer.getId())
                    .fullName("Nguyễn Văn Minh (Khách hàng)")
                    .createdAt(now)
                    .updatedAt(now)
                    .build());
        }
        // Xóa địa chỉ mặc định mẫu (1428 Lê Duẩn) nếu có để khách hàng tự nhập địa chỉ thực tế
        addressRepository.findAll().stream()
                .filter(a -> a.getAddressLine() != null && a.getAddressLine().contains("1428 Lê Duẩn"))
                .forEach(addressRepository::delete);

        // 2. STAFF (Shipper)
        User staff = ensureUser("staff", "staff@gasflow.vn", "0903888999", defaultHash, UserRole.STAFF, now);
        if (customerProfileRepository.findByUserId(staff.getId()).isEmpty()) {
            customerProfileRepository.save(CustomerProfile.builder()
                    .userId(staff.getId())
                    .fullName("Trần Tuấn Anh (Shipper)")
                    .createdAt(now)
                    .updatedAt(now)
                    .build());
        }
        StaffProfile sp1 = staffProfileRepository.findByUserId(staff.getId()).orElseGet(() ->
                staffProfileRepository.save(StaffProfile.builder()
                        .userId(staff.getId())
                        .staffStatus(StaffStatus.AVAILABLE)
                        .vehicleInfo("Xe máy Honda Wave RSX - 59P1-68686")
                        .activeDeliveryIds(new ArrayList<>())
                        .totalDeliveries(0)
                        .successDeliveries(0)
                        .failedDeliveries(0)
                        .build())
        );

        // Thêm 1 Shipper phụ để Manager dễ chọn điều phối
        User staff2 = ensureUser("shipper2", "shipper2@gasflow.vn", "0907654321", defaultHash, UserRole.STAFF, now);
        if (customerProfileRepository.findByUserId(staff2.getId()).isEmpty()) {
            customerProfileRepository.save(CustomerProfile.builder()
                    .userId(staff2.getId())
                    .fullName("Nguyễn Văn Nam (Shipper)")
                    .createdAt(now)
                    .updatedAt(now)
                    .build());
        }
        StaffProfile sp2 = staffProfileRepository.findByUserId(staff2.getId()).orElseGet(() ->
                staffProfileRepository.save(StaffProfile.builder()
                        .userId(staff2.getId())
                        .staffStatus(StaffStatus.AVAILABLE)
                        .vehicleInfo("Xe ba gác chuyên dụng - 59X2-12345")
                        .activeDeliveryIds(new ArrayList<>())
                        .totalDeliveries(0)
                        .successDeliveries(0)
                        .failedDeliveries(0)
                        .build())
        );

        // Nếu trong DB chưa có delivery nào, xóa số liệu seeding giả (12, 8) trên StaffProfile cũ
        if (deliveryRepository.count() == 0) {
            for (StaffProfile sp : List.of(sp1, sp2)) {
                if (sp.getTotalDeliveries() > 0 || sp.getSuccessDeliveries() > 0) {
                    sp.setTotalDeliveries(0);
                    sp.setSuccessDeliveries(0);
                    sp.setFailedDeliveries(0);
                    sp.setActiveDeliveryIds(new ArrayList<>());
                    sp.setStaffStatus(StaffStatus.AVAILABLE);
                    staffProfileRepository.save(sp);
                }
            }
        }

        // 3. MANAGER (Quản lý cửa hàng)
        User manager = ensureUser("manager", "manager@gasflow.vn", "0908888888", defaultHash, UserRole.MANAGER, now);
        if (customerProfileRepository.findByUserId(manager.getId()).isEmpty()) {
            customerProfileRepository.save(CustomerProfile.builder()
                    .userId(manager.getId())
                    .fullName("Lê Hoàng Quản Lý (Store Manager)")
                    .createdAt(now)
                    .updatedAt(now)
                    .build());
        }

        // 4. SYSTEM_ADMIN (Quản trị hệ thống)
        User admin = ensureUser("admin", "admin@gasflow.vn", "0909999999", defaultHash, UserRole.SYSTEM_ADMIN, now);
        if (customerProfileRepository.findByUserId(admin.getId()).isEmpty()) {
            customerProfileRepository.save(CustomerProfile.builder()
                    .userId(admin.getId())
                    .fullName("Đặng Thành Phát (System Admin)")
                    .createdAt(now)
                    .updatedAt(now)
                    .build());
        }
    }

    private User ensureUser(String username, String email, String phone, String passwordHash, UserRole role, LocalDateTime now) {
        return userRepository.findByUsername(username).orElseGet(() -> {
            User u = User.builder()
                    .username(username)
                    .email(email)
                    .phone(phone)
                    .passwordHash(passwordHash)
                    .role(role)
                    .status(AccountStatus.ACTIVE)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();
            User saved = userRepository.save(u);
            log.info("Seeded default account: username={}, role={} (password: password123)", username, role);
            return saved;
        });
    }

    private void seedDefaultCatalogAndInventory() {
        if (storeSettingRepository.findBySettingKey("DEFAULT_CONFIG").isEmpty()) {
            storeSettingRepository.save(StoreSetting.builder()
                    .settingKey("DEFAULT_CONFIG")
                    .defaultDeliveryFee(30000.0)
                    .defaultCylinderFee(250000.0)
                    .freeShippingThreshold(400000.0)
                    .storeOperatingHours("06:00-22:00")
                    .deliveryTimePolicy("Giao nhanh trong 15-30 phút nội thành")
                    .cancellationPolicy("Chỉ hủy đơn khi Manager chưa duyệt (PENDING_APPROVAL)")
                    .build());
        }

        String imgPink12 = "https://lh3.googleusercontent.com/aida-public/AB6AXuC0D3DW-X3yMwu4vP0o7LQLt7xbHgoEYQTc29a0FyFLSV9S3Bip28SbRqKI__291SDWjHjH5CDSpTsifx3luY-giW1llLnhgvdmHbr3xzMvnaXZOvjTkelueu3AVRzBkjSTsKkO2E6QjxHVp1rg_hLSeRuqYWvayi-fAHD9pJh99NkYwCAKJ3y2ijIPi10agteEBQqi9aAIWQEDj8cl8yvj03TJ26hzoWRlZJEJ5S_c_4oQAGCBBQbFHrFrbBLgjSTFHQ";
        String imgTall45 = "https://lh3.googleusercontent.com/aida-public/AB6AXuDTYmr49U5FIaK3X2y9frfAv3nFHJIjgQJd2NAd4AxHzL0LbStMUbUyYtmUX3XIP2b3ktyAoBN6jcGCpIZSiQY4KKy3zXdS_OnDgIezU5gY8tOXVDLhyx2nSDHsF6O4BXayTUkowgqjzxNrQRxWRDjeh4C9x9SdHFetUeXwFPXOtWrHAXMzSW9BihGOq2qLt5KNBOrXrLBU5LzHOIhOB2lN7VjbkjgOlReyKvApujaq9ZTq5sYLQFWSenaYbDNJ64aTIA";
        String imgBlue12 = "https://lh3.googleusercontent.com/aida-public/AB6AXuABm_QKaaSfauWeAe8j79_AT59HgVzg7CzLaPqiVbNE_mJOPbmZjaw182fVnUse1eDJykab6MfWmWrOH458DRNFsiF72uxx5m9RFn-jXq2G9in5p3z6bwfw0n4JCm0XTApkRUBUowXZsnrl37RJtW7dnyCu_1wmIPQ__G0jra__K2u02g-7rPtX4KShAAkxz_x6DQdhfpNF-4pxLyhdVrH8B9BjCMPk1AIuNC6NLMmR6o1GbfvnyEkvhOr4QAvc73gxWQ";
        String imgGrey13 = "https://lh3.googleusercontent.com/aida-public/AB6AXuArTNF83AEiHUfLgRJx6C-fPMBVebx1gTDPIe7vf_fpAaH8cVF9p-AY3xYMjLJol-GT1ZsoEqPKGjz2PoqB7JSkgFTTcBneKZswSPsUFjAjH8AJgA1vEjY-sW6E0fXIRohs30CCPUGZL3TZuuLT5aRjz8h-zCD9D4hvoOTsP3tB7JbVWdUlrPlXR5xhzhDi7sk6xvtzbcGTPsWs4GRrxUO5jFCBpJVdxwbuK6xXWQqDOEeFgKpL5hqc7oUZrEvVsA_lyA";

        // Đảm bảo mọi sản phẩm đang có trong DB đều có ảnh bình gas thật & Inventory
        List<Product> existingProducts = productRepository.findAll();
        if (!existingProducts.isEmpty()) {
            List<String> cylinderImages = List.of(imgPink12, imgTall45, imgBlue12, imgGrey13);
            for (int i = 0; i < existingProducts.size(); i++) {
                Product p = existingProducts.get(i);
                boolean changed = false;
                if (p.getName() != null && p.getName().contains("(Van chụp)")) {
                    p.setName(p.getName().replace("(Van chụp)", "").trim());
                    changed = true;
                }
                if (p.getImages() == null || p.getImages().isEmpty()
                        || p.getImages().get(0).contains("/assets/aistudio/")
                        || p.getImages().get(0).contains("Image_")) {
                    p.setImages(List.of(cylinderImages.get(i % cylinderImages.size())));
                    changed = true;
                }
                if (changed) {
                    productRepository.save(p);
                }
                if (inventoryRepository.findByProductId(p.getId()).isEmpty()) {
                    inventoryRepository.save(Inventory.builder()
                            .productId(p.getId())
                            .fullCylinder(50)
                            .emptyCylinder(15)
                            .damagedCylinder(0)
                            .reservedCylinder(0)
                            .build());
                }
            }
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        Category catDanDung = categoryRepository.save(Category.builder()
                .name("Bình Gas Dân Dụng (12kg)")
                .description("Bình gas 12kg tiêu chuẩn dùng cho hộ gia đình")
                .active(true)
                .build());
        Category catCongNghiep = categoryRepository.save(Category.builder()
                .name("Bình Gas Công Nghiệp (45kg)")
                .description("Bình gas 45kg áp suất cao dùng cho nhà hàng, bếp ăn công nghiệp")
                .active(true)
                .build());

        Brand brandPetrolimex = brandRepository.save(Brand.builder()
                .name("Petrolimex")
                .description("Tổng Công ty Gas Petrolimex")
                .active(true)
                .build());
        Brand brandPetroVietnam = brandRepository.save(Brand.builder()
                .name("PetroVietnam Gas")
                .description("Tổng Công ty Khí Việt Nam - PV Gas")
                .active(true)
                .build());
        Brand brandSaigonPetro = brandRepository.save(Brand.builder()
                .name("Saigon Petro")
                .description("Công ty TNHH MTV Dầu khí TP.HCM")
                .active(true)
                .build());

        Product p1 = productRepository.save(Product.builder()
                .name("Bình Gas Petrolimex 12kg")
                .brandId(brandPetrolimex.getId())
                .categoryId(catDanDung.getId())
                .gasType("LPG Dân dụng")
                .weightKg(12.0)
                .gasPrice(450000.0)
                .cylinderFee(250000.0)
                .description("Bình gas Petrolimex 12kg chính hãng màu xanh da trời, tích hợp tem chống giả và bảo hiểm an toàn.")
                .images(List.of(imgBlue12))
                .status(ProductStatus.ACTIVE)
                .createdAt(now)
                .updatedAt(now)
                .build());

        Product p2 = productRepository.save(Product.builder()
                .name("Bình Gas Saigon Petro 12kg Xám")
                .brandId(brandSaigonPetro.getId())
                .categoryId(catDanDung.getId())
                .gasType("LPG Dân dụng")
                .weightKg(12.0)
                .gasPrice(430000.0)
                .cylinderFee(250000.0)
                .description("Bình gas Saigon Petro 12kg chính hãng an toàn, lửa xanh tiết kiệm gas.")
                .images(List.of(imgGrey13))
                .status(ProductStatus.ACTIVE)
                .createdAt(now)
                .updatedAt(now)
                .build());

        Product p3 = productRepository.save(Product.builder()
                .name("Bình Gas Công Nghiệp PetroVietnam 45kg")
                .brandId(brandPetroVietnam.getId())
                .categoryId(catCongNghiep.getId())
                .gasType("LPG Công nghiệp")
                .weightKg(45.0)
                .gasPrice(1650000.0)
                .cylinderFee(900000.0)
                .description("Bình gas công nghiệp 45kg chuyên dụng cho nhà hàng, khách sạn, xưởng sản xuất.")
                .images(List.of(imgTall45))
                .status(ProductStatus.ACTIVE)
                .createdAt(now)
                .updatedAt(now)
                .build());

        for (Product p : List.of(p1, p2, p3)) {
            inventoryRepository.save(Inventory.builder()
                    .productId(p.getId())
                    .fullCylinder(60)
                    .emptyCylinder(20)
                    .damagedCylinder(1)
                    .reservedCylinder(0)
                    .build());
        }

        log.info("Seeded default categories, brands, products and inventories.");
    }
}

