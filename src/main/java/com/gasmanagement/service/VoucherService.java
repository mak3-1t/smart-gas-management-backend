package com.gasmanagement.service;

import com.gasmanagement.dto.request.VoucherRequest;
import com.gasmanagement.dto.response.VoucherResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface VoucherService {

    // --- MANAGER ENDPOINTS ---

    /**
     * Tạo Voucher mới (Manager).
     */
    VoucherResponse createVoucher(VoucherRequest request);

    /**
     * Cập nhật thông tin Voucher (Manager).
     */
    VoucherResponse updateVoucher(String id, VoucherRequest request);

    /**
     * Thay đổi trạng thái Voucher (Manager).
     * @param id ID của Voucher
     * @param status Trạng thái mới (ACTIVE, INACTIVE)
     */
    VoucherResponse changeStatus(String id, String status);

    /**
     * Lấy danh sách Voucher có phân trang, hỗ trợ lọc theo trạng thái (Manager).
     */
    Page<VoucherResponse> getAllVouchers(String status, Pageable pageable);

    // --- CUSTOMER ENDPOINTS ---

    /**
     * Lấy danh sách các Voucher hợp lệ có thể sử dụng ở thời điểm hiện tại (Customer).
     * Điều kiện: ACTIVE, trong thời gian hiệu lực, chưa hết lượt sử dụng.
     */
    List<VoucherResponse> getUsableVouchers();

    /**
     * Tính toán số tiền được giảm giá (Customer/Checkout).
     * @param code Mã Voucher
     * @param orderTotal Tổng tiền đơn hàng chưa giảm
     * @return Số tiền được giảm
     */
    Double calculateDiscount(String code, Double orderTotal);
    
    /**
     * Tăng số lượng đã sử dụng của Voucher sau khi đặt hàng thành công.
     */
    void incrementUsage(String code);
}
