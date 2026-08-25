package com.gasmanagement.service.impl;

import com.gasmanagement.dto.request.VoucherRequest;
import com.gasmanagement.dto.response.VoucherResponse;
import com.gasmanagement.exception.BusinessException;
import com.gasmanagement.exception.ResourceNotFoundException;
import com.gasmanagement.model.Voucher;
import com.gasmanagement.model.enums.DiscountType;
import com.gasmanagement.repository.VoucherRepository;
import com.gasmanagement.service.VoucherService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VoucherServiceImpl implements VoucherService {

    private final VoucherRepository voucherRepository;

    @Override
    public VoucherResponse createVoucher(VoucherRequest request) {
        if (voucherRepository.findByCode(request.getCode()).isPresent()) {
            throw new BusinessException("Voucher code đã tồn tại: " + request.getCode());
        }

        validateVoucherRequest(request);

        Voucher voucher = Voucher.builder()
                .code(request.getCode())
                .discountType(request.getDiscountType())
                .discountValue(request.getDiscountValue())
                .minimumOrder(request.getMinimumOrder())
                .maximumDiscount(request.getMaximumDiscount())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .usageLimit(request.getUsageLimit())
                .usedCount(0)
                .status("ACTIVE")
                .build();

        Voucher savedVoucher = voucherRepository.save(voucher);
        return mapToResponse(savedVoucher);
    }

    @Override
    public VoucherResponse updateVoucher(String id, VoucherRequest request) {
        Voucher voucher = voucherRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy voucher: " + id));

        // Nếu đổi code thì kiểm tra code mới có trùng không
        if (!voucher.getCode().equals(request.getCode())) {
            if (voucherRepository.findByCode(request.getCode()).isPresent()) {
                throw new BusinessException("Voucher code đã tồn tại: " + request.getCode());
            }
        }

        validateVoucherRequest(request);

        voucher.setCode(request.getCode());
        voucher.setDiscountType(request.getDiscountType());
        voucher.setDiscountValue(request.getDiscountValue());
        voucher.setMinimumOrder(request.getMinimumOrder());
        voucher.setMaximumDiscount(request.getMaximumDiscount());
        voucher.setStartDate(request.getStartDate());
        voucher.setEndDate(request.getEndDate());
        voucher.setUsageLimit(request.getUsageLimit());

        Voucher savedVoucher = voucherRepository.save(voucher);
        return mapToResponse(savedVoucher);
    }

    @Override
    public VoucherResponse changeStatus(String id, String status) {
        Voucher voucher = voucherRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy voucher: " + id));

        if (!status.equals("ACTIVE") && !status.equals("INACTIVE")) {
            throw new BusinessException("Trạng thái không hợp lệ. Chỉ chấp nhận ACTIVE hoặc INACTIVE");
        }

        voucher.setStatus(status);
        Voucher savedVoucher = voucherRepository.save(voucher);
        return mapToResponse(savedVoucher);
    }

    @Override
    public Page<VoucherResponse> getAllVouchers(String status, Pageable pageable) {
        Page<Voucher> vouchers;
        if (status != null && !status.trim().isEmpty()) {
            vouchers = voucherRepository.findByStatus(status.toUpperCase(), pageable);
        } else {
            vouchers = voucherRepository.findAll(pageable);
        }
        return vouchers.map(this::mapToResponse);
    }

    @Override
    public List<VoucherResponse> getUsableVouchers() {
        LocalDateTime now = LocalDateTime.now();
        List<Voucher> usableVouchers = voucherRepository.findUsableVouchers(now);
        return usableVouchers.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public Double calculateDiscount(String code, Double orderTotal) {
        Voucher voucher = voucherRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy voucher: " + code));

        LocalDateTime now = LocalDateTime.now();

        // 1. Check status
        if (!"ACTIVE".equals(voucher.getStatus())) {
            throw new BusinessException("Voucher không hoạt động");
        }

        // 2. Check date
        if (now.isBefore(voucher.getStartDate())) {
            throw new BusinessException("Voucher chưa đến ngày sử dụng");
        }
        if (now.isAfter(voucher.getEndDate())) {
            throw new BusinessException("Voucher đã hết hạn sử dụng");
        }

        // 3. Check limit
        if (voucher.getUsedCount() >= voucher.getUsageLimit()) {
            throw new BusinessException("Voucher đã hết lượt sử dụng");
        }

        // 4. Check minimum order
        if (orderTotal < voucher.getMinimumOrder()) {
            throw new BusinessException("Đơn hàng chưa đạt giá trị tối thiểu để áp dụng voucher này (" + voucher.getMinimumOrder() + ")");
        }

        // Calculate discount
        double discount = 0.0;
        if (voucher.getDiscountType() == DiscountType.FIXED_AMOUNT) {
            discount = voucher.getDiscountValue();
        } else if (voucher.getDiscountType() == DiscountType.PERCENTAGE) {
            discount = orderTotal * (voucher.getDiscountValue() / 100.0);
            if (voucher.getMaximumDiscount() != null && voucher.getMaximumDiscount() > 0) {
                discount = Math.min(discount, voucher.getMaximumDiscount());
            }
        }

        // Không giảm quá số tiền đơn hàng
        return Math.min(discount, orderTotal);
    }

    @Override
    public void incrementUsage(String code) {
        Voucher voucher = voucherRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy voucher: " + code));
        
        voucher.setUsedCount(voucher.getUsedCount() + 1);
        voucherRepository.save(voucher);
    }

    private void validateVoucherRequest(VoucherRequest request) {
        if (request.getStartDate().isAfter(request.getEndDate())) {
            throw new BusinessException("Ngày bắt đầu không được sau ngày kết thúc");
        }
        if (request.getDiscountType() == DiscountType.PERCENTAGE) {
            if (request.getDiscountValue() > 100) {
                throw new BusinessException("Giảm giá theo phần trăm không được vượt quá 100%");
            }
            if (request.getMaximumDiscount() == null || request.getMaximumDiscount() < 0) {
                throw new BusinessException("Phải thiết lập mức giảm giá tối đa (maximumDiscount) hợp lệ khi dùng PERCENTAGE");
            }
        }
    }

    private VoucherResponse mapToResponse(Voucher voucher) {
        LocalDateTime now = LocalDateTime.now();
        boolean isUsable = "ACTIVE".equals(voucher.getStatus()) &&
                !now.isBefore(voucher.getStartDate()) &&
                !now.isAfter(voucher.getEndDate()) &&
                voucher.getUsedCount() < voucher.getUsageLimit();

        return VoucherResponse.builder()
                .id(voucher.getId())
                .code(voucher.getCode())
                .discountType(voucher.getDiscountType())
                .discountValue(voucher.getDiscountValue())
                .minimumOrder(voucher.getMinimumOrder())
                .maximumDiscount(voucher.getMaximumDiscount())
                .startDate(voucher.getStartDate())
                .endDate(voucher.getEndDate())
                .usageLimit(voucher.getUsageLimit())
                .usedCount(voucher.getUsedCount())
                .status(voucher.getStatus())
                .isUsable(isUsable)
                .build();
    }
}
