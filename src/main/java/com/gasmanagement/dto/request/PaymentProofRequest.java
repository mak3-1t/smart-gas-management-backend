package com.gasmanagement.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentProofRequest {

    @NotBlank(message = "URL bằng chứng thanh toán không được để trống")
    private String paymentProofUrl;
}
