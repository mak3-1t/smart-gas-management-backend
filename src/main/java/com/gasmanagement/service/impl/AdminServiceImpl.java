package com.gasmanagement.service.impl;

import com.gasmanagement.dto.request.AccountStatusUpdateRequest;
import com.gasmanagement.dto.request.RegisterRequest;
import com.gasmanagement.dto.request.RoleUpdateRequest;
import com.gasmanagement.dto.response.CustomerSummaryResponse;
import com.gasmanagement.dto.response.admin.AuditLogResponse;
import com.gasmanagement.exception.BusinessException;
import com.gasmanagement.exception.ResourceNotFoundException;
import com.gasmanagement.model.*;
import com.gasmanagement.model.enums.*;
import com.gasmanagement.repository.*;
import com.gasmanagement.service.AdminService;
import com.gasmanagement.service.AuditService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final StaffProfileRepository staffProfileRepository;
    private final AddressRepository addressRepository;
    private final OrderRepository orderRepository;
    private final DeliveryRepository deliveryRepository;
    private final PaymentRepository paymentRepository;
    private final CartRepository cartRepository;
    private final InventoryRepository inventoryRepository;
    private final AuditLogRepository auditLogRepository;
    private final AuditService auditService;
    private final PasswordEncoder passwordEncoder;

    @Override
    public Page<User> getAllAccounts(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    @Override
    public List<CustomerSummaryResponse> getCustomerSummaries(String keyword) {
        List<User> customerUsers = userRepository.findByRole(UserRole.CUSTOMER);
        if (customerUsers.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, CustomerProfile> profileByUserId = customerProfileRepository.findAll().stream()
                .filter(p -> p.getUserId() != null)
                .collect(Collectors.toMap(CustomerProfile::getUserId, Function.identity(), (a, b) -> b));

        Map<String, List<Address>> addressesByUserId = addressRepository.findAll().stream()
                .filter(a -> a.getUserId() != null)
                .collect(Collectors.groupingBy(Address::getUserId));

        Map<String, List<Order>> ordersByCustomerId = orderRepository.findAll().stream()
                .filter(o -> o.getCustomerId() != null)
                .collect(Collectors.groupingBy(Order::getCustomerId));

        String kw = keyword != null ? keyword.trim().toLowerCase() : "";
        List<CustomerSummaryResponse> result = new ArrayList<>();

        for (User u : customerUsers) {
            CustomerProfile profile = profileByUserId.get(u.getId());
            String fullName = (profile != null && profile.getFullName() != null && !profile.getFullName().isBlank())
                    ? profile.getFullName()
                    : u.getUsername();

            List<Order> userOrders = ordersByCustomerId.getOrDefault(u.getId(), Collections.emptyList());
            long totalOrders = userOrders.size();
            long deliveredOrders = userOrders.stream()
                    .filter(o -> o.getOrderStatus() == OrderStatus.DELIVERED)
                    .count();
            double totalSpent = userOrders.stream()
                    .filter(o -> o.getOrderStatus() != OrderStatus.CANCELLED
                            && o.getApprovalStatus() != ApprovalStatus.REJECTED)
                    .mapToDouble(o -> o.getFinalAmount() != null ? o.getFinalAmount() : 0.0)
                    .sum();
            LocalDateTime lastOrderDate = userOrders.stream()
                    .map(Order::getCreatedAt)
                    .filter(Objects::nonNull)
                    .max(LocalDateTime::compareTo)
                    .orElse(null);

            String addressText = "";
            List<Address> userAddrs = addressesByUserId.getOrDefault(u.getId(), Collections.emptyList());
            Address defaultAddr = userAddrs.stream().filter(Address::isDefault).findFirst()
                    .orElse(userAddrs.isEmpty() ? null : userAddrs.get(0));
            if (defaultAddr != null) {
                addressText = Arrays.asList(
                        defaultAddr.getAddressLine(),
                        defaultAddr.getWard(),
                        defaultAddr.getDistrict(),
                        defaultAddr.getCity()).stream().filter(s -> s != null && !s.isBlank())
                        .collect(Collectors.joining(", "));
            } else if (!userOrders.isEmpty()) {
                Order latestWithAddr = userOrders.stream()
                        .filter(o -> o.getDeliveryAddressSnapshot() != null)
                        .max(Comparator.comparing(o -> o.getCreatedAt() != null ? o.getCreatedAt() : LocalDateTime.MIN))
                        .orElse(null);
                if (latestWithAddr != null && latestWithAddr.getDeliveryAddressSnapshot() != null) {
                    Order.AddressSnapshot snap = latestWithAddr.getDeliveryAddressSnapshot();
                    addressText = Arrays.asList(
                            snap.getAddressLine(),
                            snap.getWard(),
                            snap.getDistrict(),
                            snap.getCity()).stream().filter(s -> s != null && !s.isBlank())
                            .collect(Collectors.joining(", "));
                }
            }

            if (!kw.isEmpty()) {
                boolean match = (fullName != null && fullName.toLowerCase().contains(kw))
                        || (u.getUsername() != null && u.getUsername().toLowerCase().contains(kw))
                        || (u.getPhone() != null && u.getPhone().toLowerCase().contains(kw))
                        || (u.getEmail() != null && u.getEmail().toLowerCase().contains(kw));
                if (!match)
                    continue;
            }

            result.add(CustomerSummaryResponse.builder()
                    .id(u.getId())
                    .userId(u.getId())
                    .username(u.getUsername())
                    .fullName(fullName)
                    .email(u.getEmail())
                    .phone(u.getPhone())
                    .status(u.getStatus())
                    .address(addressText)
                    .totalOrders(totalOrders)
                    .deliveredOrders(deliveredOrders)
                    .totalSpent(totalSpent)
                    .lastOrderDate(lastOrderDate)
                    .createdAt(u.getCreatedAt())
                    .build());
        }

        result.sort((a, b) -> {
            LocalDateTime da = a.getLastOrderDate() != null ? a.getLastOrderDate() : a.getCreatedAt();
            LocalDateTime db = b.getLastOrderDate() != null ? b.getLastOrderDate() : b.getCreatedAt();
            if (da == null && db == null)
                return 0;
            if (da == null)
                return 1;
            if (db == null)
                return -1;
            return db.compareTo(da);
        });

        return result;
    }

    @Override
    public User createAccount(RegisterRequest request, String adminId, HttpServletRequest httpRequest) {
        User actor = adminId != null ? userRepository.findById(adminId).orElse(null) : null;
        UserRole actorRole = actor != null ? actor.getRole() : UserRole.MANAGER;
        UserRole role = request.getRole() != null ? request.getRole() : UserRole.CUSTOMER;

        if ((role == UserRole.MANAGER || role == UserRole.SYSTEM_ADMIN) && actorRole != UserRole.SYSTEM_ADMIN) {
            throw new BusinessException(
                    "Chỉ Quản trị viên hệ thống (SYSTEM_ADMIN) mới có quyền tạo tài khoản Quản lý hoặc Admin!");
        }

        if (userRepository.existsByUsername(request.getUsername().trim())) {
            throw new BusinessException("Tên đăng nhập '" + request.getUsername() + "' đã tồn tại");
        }
        if (userRepository.existsByEmail(request.getEmail().trim())) {
            throw new BusinessException("Email '" + request.getEmail() + "' đã tồn tại");
        }

        LocalDateTime now = LocalDateTime.now();

        User user = User.builder()
                .username(request.getUsername().trim())
                .email(request.getEmail().trim())
                .phone(request.getPhone())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(role)
                .status(AccountStatus.ACTIVE)
                .createdAt(now)
                .updatedAt(now)
                .build();

        User saved = userRepository.save(user);

        customerProfileRepository.save(CustomerProfile.builder()
                .userId(saved.getId())
                .fullName(request.getFullName() != null && !request.getFullName().isBlank()
                        ? request.getFullName().trim()
                        : saved.getUsername())
                .createdAt(now)
                .updatedAt(now)
                .build());

        if (role == UserRole.STAFF && staffProfileRepository.findByUserId(saved.getId()).isEmpty()) {
            String vehicle = (request.getVehicleInfo() != null && !request.getVehicleInfo().isBlank())
                    ? request.getVehicleInfo().trim()
                    : "Xe máy chuyên dụng";
            staffProfileRepository.save(StaffProfile.builder()
                    .userId(saved.getId())
                    .staffStatus(StaffStatus.AVAILABLE)
                    .vehicleInfo(vehicle)
                    .activeDeliveryIds(new ArrayList<>())
                    .totalDeliveries(0)
                    .successDeliveries(0)
                    .failedDeliveries(0)
                    .build());
        }

        auditService.logAction(adminId, actorRole, "CREATE_ACCOUNT",
                "User", saved.getId(), null, role.name(), httpRequest);
        return saved;
    }

    @Override
    public List<CustomerSummaryResponse> getManagementAccounts() {
        List<User> allUsers = userRepository.findAll();
        Map<String, CustomerProfile> profileByUserId = customerProfileRepository.findAll().stream()
                .filter(p -> p.getUserId() != null)
                .collect(Collectors.toMap(CustomerProfile::getUserId, Function.identity(), (a, b) -> b));

        List<CustomerSummaryResponse> list = new ArrayList<>();
        for (User u : allUsers) {
            if (u.getRole() == UserRole.CUSTOMER)
                continue;
            CustomerProfile prof = profileByUserId.get(u.getId());
            String fullName = (prof != null && prof.getFullName() != null && !prof.getFullName().isBlank())
                    ? prof.getFullName()
                    : u.getUsername();
            list.add(CustomerSummaryResponse.builder()
                    .id(u.getId())
                    .userId(u.getId())
                    .username(u.getUsername())
                    .fullName(fullName)
                    .email(u.getEmail())
                    .phone(u.getPhone())
                    .status(u.getStatus())
                    .address(u.getRole() != null ? u.getRole().name() : "MANAGER")
                    .createdAt(u.getCreatedAt())
                    .build());
        }
        return list;
    }

    @Override
    public User resetUserPassword(String userId, String newPassword, String actorId, HttpServletRequest httpRequest) {
        if (newPassword == null || newPassword.trim().length() < 6) {
            throw new BusinessException("Mật khẩu mới phải có ít nhất 6 ký tự");
        }
        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản với ID: " + userId));

        User actor = actorId != null ? userRepository.findById(actorId).orElse(null) : null;
        UserRole actorRole = actor != null ? actor.getRole() : UserRole.MANAGER;

        if ((targetUser.getRole() == UserRole.MANAGER || targetUser.getRole() == UserRole.SYSTEM_ADMIN)
                && actorRole != UserRole.SYSTEM_ADMIN) {
            throw new BusinessException(
                    "Chỉ Quản trị viên hệ thống (SYSTEM_ADMIN) mới có quyền đổi mật khẩu của Quản lý / Admin");
        }

        targetUser.setPasswordHash(passwordEncoder.encode(newPassword.trim()));
        targetUser.setUpdatedAt(LocalDateTime.now());
        User saved = userRepository.save(targetUser);

        auditService.logAction(actorId, actorRole, "RESET_PASSWORD",
                "User", userId, null, "Password reset by " + actorRole, httpRequest);
        return saved;
    }

    @Override
    public void deleteAccount(String userId, String actorId, HttpServletRequest httpRequest) {
        if (userId.equals(actorId)) {
            throw new BusinessException("Bạn không thể tự xóa tài khoản đang đăng nhập của chính mình");
        }
        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản với ID: " + userId));

        User actor = actorId != null ? userRepository.findById(actorId).orElse(null) : null;
        UserRole actorRole = actor != null ? actor.getRole() : UserRole.MANAGER;

        if (targetUser.getRole() != UserRole.CUSTOMER && actorRole != UserRole.SYSTEM_ADMIN) {
            throw new BusinessException(
                    "Chỉ Quản trị viên hệ thống (SYSTEM_ADMIN) mới có quyền xóa tài khoản Quản lý / Nhân sự");
        }

        if (targetUser.getRole() == UserRole.SYSTEM_ADMIN) {
            long adminCount = userRepository.findByRole(UserRole.SYSTEM_ADMIN).size();
            if (adminCount <= 1) {
                throw new BusinessException("Không thể xóa tài khoản SYSTEM_ADMIN cuối cùng trên hệ thống");
            }
        }

        customerProfileRepository.findByUserId(userId).ifPresent(customerProfileRepository::delete);
        staffProfileRepository.findByUserId(userId).ifPresent(staffProfileRepository::delete);
        List<Address> addrs = addressRepository.findByUserId(userId);
        if (addrs != null && !addrs.isEmpty()) {
            addressRepository.deleteAll(addrs);
        }
        cartRepository.findByUserId(userId).ifPresent(cartRepository::delete);
        userRepository.delete(targetUser);

        auditService.logAction(actorId, actorRole, "DELETE_ACCOUNT",
                "User", userId, targetUser.getUsername(), "DELETED", httpRequest);
    }

    @Override
    public Map<String, Object> resetTransactionalData() {
        long deletedOrders = orderRepository.count();
        long deletedDeliveries = deliveryRepository.count();
        long deletedPayments = paymentRepository.count();

        orderRepository.deleteAll();
        deliveryRepository.deleteAll();
        paymentRepository.deleteAll();
        cartRepository.deleteAll();

        List<StaffProfile> staffs = staffProfileRepository.findAll();
        for (StaffProfile sp : staffs) {
            sp.setStaffStatus(StaffStatus.AVAILABLE);
            sp.setActiveDeliveryIds(new ArrayList<>());
            sp.setTotalDeliveries(0);
            sp.setSuccessDeliveries(0);
            sp.setFailedDeliveries(0);
            staffProfileRepository.save(sp);
        }

        List<Inventory> inventories = inventoryRepository.findAll();
        for (Inventory inv : inventories) {
            inv.setReservedCylinder(0);
            inventoryRepository.save(inv);
        }

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("deletedOrders", deletedOrders);
        summary.put("deletedDeliveries", deletedDeliveries);
        summary.put("deletedPayments", deletedPayments);
        summary.put("resetStaffProfiles", staffs.size());
        return summary;
    }

    @Override
    public User updateAccountStatus(String userId, AccountStatusUpdateRequest request, String adminId,
            HttpServletRequest httpRequest) {
        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        // Kiểm tra không cho admin tự block chính mình
        if (userId.equals(adminId)) {
            throw new BusinessException("You cannot change your own account status");
        }

        // Parse new status
        AccountStatus newStatus;
        try {
            newStatus = AccountStatus.valueOf(request.getStatus().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Invalid account status. Allowed values: ACTIVE, LOCKED, DISABLED");
        }

        AccountStatus oldStatus = targetUser.getStatus();

        if (oldStatus == newStatus) {
            return targetUser; // Không có gì thay đổi
        }

        // Nếu SYSTEM_ADMIN cuối cùng đang active thì không cho disable/lock
        if (targetUser.getRole() == UserRole.SYSTEM_ADMIN && newStatus != AccountStatus.ACTIVE) {
            long activeAdmins = userRepository.findAll().stream()
                    .filter(u -> u.getRole() == UserRole.SYSTEM_ADMIN && u.getStatus() == AccountStatus.ACTIVE)
                    .count();
            if (activeAdmins <= 1) {
                throw new BusinessException("Cannot disable/lock the last active SYSTEM_ADMIN");
            }
        }

        // Cập nhật status
        targetUser.setStatus(newStatus);
        targetUser.setUpdatedAt(LocalDateTime.now());
        User savedUser = userRepository.save(targetUser);

        // Ghi Audit Log
        String actionName = "UPDATE_ACCOUNT_STATUS";
        String auditOldValue = oldStatus.name();
        String auditNewValue = newStatus.name()
                + (request.getReason() != null ? " (Reason: " + request.getReason() + ")" : "");

        auditService.logAction(adminId, UserRole.SYSTEM_ADMIN, actionName, "User", userId, auditOldValue, auditNewValue,
                httpRequest);

        log.info("Admin {} changed status of user {} from {} to {}", adminId, userId, oldStatus, newStatus);
        return savedUser;
    }

    @Override
    public User updateAccountRole(String userId, RoleUpdateRequest request, String adminId,
            HttpServletRequest httpRequest) {
        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        // Kiểm tra không cho admin tự đổi role chính mình
        if (userId.equals(adminId)) {
            throw new BusinessException("You cannot change your own role");
        }

        // Parse new role
        UserRole newRole;
        try {
            newRole = UserRole.valueOf(request.getRole().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Invalid role. Allowed values: CUSTOMER, STAFF, MANAGER, SYSTEM_ADMIN");
        }

        UserRole oldRole = targetUser.getRole();

        if (oldRole == newRole) {
            return targetUser; // Không có gì thay đổi
        }

        // Cập nhật role
        targetUser.setRole(newRole);
        targetUser.setUpdatedAt(LocalDateTime.now());
        User savedUser = userRepository.save(targetUser);

        if (newRole == UserRole.STAFF && staffProfileRepository.findByUserId(userId).isEmpty()) {
            staffProfileRepository.save(StaffProfile.builder()
                    .userId(userId)
                    .staffStatus(StaffStatus.AVAILABLE)
                    .vehicleInfo("Xe máy chuyên dụng")
                    .activeDeliveryIds(new ArrayList<>())
                    .totalDeliveries(0)
                    .successDeliveries(0)
                    .failedDeliveries(0)
                    .build());
        }

        // Ghi Audit Log
        String actionName = "UPDATE_ACCOUNT_ROLE";
        String auditOldValue = oldRole.name();
        String auditNewValue = newRole.name()
                + (request.getReason() != null ? " (Reason: " + request.getReason() + ")" : "");

        auditService.logAction(adminId, UserRole.SYSTEM_ADMIN, actionName, "User", userId, auditOldValue, auditNewValue,
                httpRequest);

        log.info("Admin {} changed role of user {} from {} to {}", adminId, userId, oldRole, newRole);
        return savedUser;
    }

    @Override
    public Page<AuditLogResponse> getAuditLogs(Pageable pageable) {
        return auditLogRepository.findAll(pageable).map(this::mapToAuditLogResponse);
    }

    @Override
    public Page<AuditLogResponse> getAuditLogsByEntity(String entityType, String entityId, Pageable pageable) {
        return auditLogRepository.findByEntityTypeAndEntityId(entityType, entityId, pageable)
                .map(this::mapToAuditLogResponse);
    }

    @Override
    public Page<AuditLogResponse> getAuditLogsByAction(String action, Pageable pageable) {
        return auditLogRepository.findByAction(action, pageable).map(this::mapToAuditLogResponse);
    }

    private AuditLogResponse mapToAuditLogResponse(AuditLog auditLog) {
        String username = "Unknown";
        if (auditLog.getActorId() != null) {
            username = userRepository.findById(auditLog.getActorId())
                    .map(User::getUsername)
                    .orElse("Unknown");
        }

        return AuditLogResponse.builder()
                .id(auditLog.getId())
                .actorId(auditLog.getActorId())
                .actorUsername(username)
                .actorRole(auditLog.getActorRole())
                .action(auditLog.getAction())
                .entityType(auditLog.getEntityType())
                .entityId(auditLog.getEntityId())
                .oldValue(auditLog.getOldValue())
                .newValue(auditLog.getNewValue())
                .ipAddress(auditLog.getIpAddress())
                .createdAt(auditLog.getCreatedAt())
                .build();
    }
}
