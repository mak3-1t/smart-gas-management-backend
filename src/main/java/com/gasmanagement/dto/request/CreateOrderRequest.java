package com.gasmanagement.dto.request;

import com.gasmanagement.model.Order;
import com.gasmanagement.model.enums.DeliveryType;
import com.gasmanagement.model.enums.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderRequest {

    /** Chọn từ địa chỉ đã lưu trong danh sách của Customer */
    private String addressId;

    /** Hoặc cung cấp địa chỉ trực tiếp lúc đặt nếu không chọn addressId */
    private Order.AddressSnapshot customAddress;

    @NotNull(message = "Phương thức giao hàng (DELIVER_NOW / SCHEDULED_DELIVERY) không được để trống")
    private DeliveryType deliveryType;

    /** Bắt buộc nếu deliveryType = SCHEDULED_DELIVERY */
    private LocalDateTime scheduledDeliveryTime;

    @NotNull(message = "Phương thức thanh toán không được để trống")
    private PaymentMethod paymentMethod;

    /** Mã voucher (nếu chưa áp dụng trước đó trong Cart) */
    private String voucherCode;

    private String note;
}
