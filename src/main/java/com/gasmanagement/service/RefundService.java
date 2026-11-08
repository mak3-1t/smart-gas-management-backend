package com.gasmanagement.service;

import com.gasmanagement.dto.request.RefundProcessRequest;
import com.gasmanagement.dto.response.RefundResponse;
import com.gasmanagement.model.enums.RefundStatus;

import java.util.List;

public interface RefundService {
    List<RefundResponse> getAllRefunds(RefundStatus status);
    RefundResponse getRefundById(String refundId);
    RefundResponse processRefund(String refundId, String managerId, RefundProcessRequest request);
}
