package com.gasmanagement.service;

import com.gasmanagement.dto.response.PaymentResponse;
import com.gasmanagement.model.Order;
import com.gasmanagement.model.Payment;
import com.gasmanagement.model.enums.OrderStatus;
import com.gasmanagement.model.enums.PaymentMethod;
import com.gasmanagement.model.enums.PaymentStatus;
import com.gasmanagement.repository.OrderRepository;
import com.gasmanagement.repository.PaymentRepository;
import com.gasmanagement.service.impl.PaymentServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    @Test
    @DisplayName("Contract 3: Staff xác nhận thanh toán COD -> Cập nhật Payment = PAID và Order = PAID")
    void testConfirmPayment_UpdatesPaymentAndOrderToPaid() {
        String paymentId = "pay-001";
        String orderId = "ord-001";
        String staffId = "staff-99";

        Payment payment = Payment.builder()
                .id(paymentId)
                .orderId(orderId)
                .customerId("cust-1")
                .paymentMethod(PaymentMethod.COD)
                .paymentStatus(PaymentStatus.PENDING)
                .amount(480000.0)
                .paidAmount(0.0)
                .transactions(new ArrayList<>())
                .build();

        Order order = Order.builder()
                .id(orderId)
                .orderCode("GAS-TEST-01")
                .customerId("cust-1")
                .orderStatus(OrderStatus.ORDER_PLACED)
                .paymentStatus(PaymentStatus.PENDING)
                .finalAmount(480000.0)
                .statusHistory(new ArrayList<>())
                .build();

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        PaymentResponse response = paymentService.confirmPayment(paymentId, staffId, "Thu tiền COD tại nhà");

        assertNotNull(response);
        assertEquals(PaymentStatus.PAID, response.getPaymentStatus());
        assertEquals(480000.0, response.getPaidAmount());
        assertEquals(staffId, response.getConfirmedByStaffId());

        // Kiểm tra order cũng được cập nhật PAID
        assertEquals(PaymentStatus.PAID, order.getPaymentStatus());
        assertEquals(OrderStatus.PAID, order.getOrderStatus());

        verify(paymentRepository, times(1)).save(payment);
        verify(orderRepository, times(1)).save(order);
    }
}
