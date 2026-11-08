package com.gasmanagement.repository;

import com.gasmanagement.model.VoucherUsage;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VoucherUsageRepository extends MongoRepository<VoucherUsage, String> {
    List<VoucherUsage> findByCustomerId(String customerId);
    List<VoucherUsage> findByVoucherId(String voucherId);
    boolean existsByVoucherIdAndCustomerId(String voucherId, String customerId);
}
