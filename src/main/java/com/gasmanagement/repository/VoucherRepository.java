package com.gasmanagement.repository;

import com.gasmanagement.model.Voucher;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface VoucherRepository extends MongoRepository<Voucher, String> {
    Optional<Voucher> findByCode(String code);

    Page<Voucher> findByStatus(String status, Pageable pageable);

    @Query("{ 'status': 'ACTIVE', 'startDate': { $lte: ?0 }, 'endDate': { $gte: ?0 }, $expr: { $lt: ['$usedCount', '$usageLimit'] } }")
    List<Voucher> findUsableVouchers(LocalDateTime now);
}
